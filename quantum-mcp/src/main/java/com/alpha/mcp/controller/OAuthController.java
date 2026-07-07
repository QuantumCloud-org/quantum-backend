package com.alpha.mcp.controller;

import com.alpha.framework.context.UserContext;
import com.alpha.framework.entity.LoginUser;
import com.alpha.framework.enums.ResultCode;
import com.alpha.framework.exception.BizException;
import com.alpha.mcp.config.McpProperties;
import com.alpha.mcp.oauth.OAuthTokenResponse;
import com.alpha.mcp.oauth.OAuthTokenService;
import com.alpha.mcp.oauth.PkceUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ai.mcp", name = "enabled", havingValue = "true")
public class OAuthController {

    private final OAuthTokenService tokenService;
    private final McpProperties properties;

    @GetMapping("/oauth/authorize")
    public ResponseEntity<?> authorize(
            @RequestParam("response_type") String responseType,
            @RequestParam("client_id") String clientId,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam("code_challenge") String codeChallenge,
            @RequestParam("code_challenge_method") String codeChallengeMethod,
            @RequestParam(value = "resource", required = false) String resource,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "consent", required = false) String consent) {
        if (!"code".equals(responseType)) {
            throw new BizException(ResultCode.PARAM_INVALID, "only response_type=code is supported");
        }
        LoginUser loginUser = UserContext.getUser();
        if (loginUser == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "login is required before consent");
        }
        Set<String> scopes = parseScopes(scope);
        if (!"approve".equals(consent)) {
            McpProperties.Client client = requireClient(clientId);
            return ResponseEntity.ok(Map.of(
                    "consentRequired", true,
                    "clientId", clientId,
                    "clientName", client.getName(),
                    "resource", properties.normalizedResource(resource),
                    "scopes", scopes.isEmpty() ? properties.getDefaultScopes() : scopes,
                    "readOnly", true
            ));
        }

        String code = tokenService.createAuthorizationCode(
                new OAuthTokenService.AuthorizationRequest(
                        clientId,
                        redirectUri,
                        codeChallenge,
                        codeChallengeMethod,
                        properties.normalizedResource(resource),
                        scopes,
                        state),
                loginUser);
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("code", code);
        if (state != null && !state.isBlank()) {
            builder.queryParam("state", state);
        }
        URI redirect = builder.build(true).toUri();
        return ResponseEntity.status(HttpStatus.FOUND).location(redirect).build();
    }

    @PostMapping(value = "/oauth/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Map<String, Object> token(
            @RequestParam("grant_type") String grantType,
            @RequestParam("client_id") String clientId,
            @RequestParam(value = "redirect_uri", required = false) String redirectUri,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "code_verifier", required = false) String codeVerifier,
            @RequestParam(value = "refresh_token", required = false) String refreshToken,
            @RequestParam(value = "resource", required = false) String resource) {
        OAuthTokenResponse tokenResponse;
        if ("authorization_code".equals(grantType)) {
            tokenResponse = tokenService.exchangeAuthorizationCode(
                    clientId,
                    redirectUri,
                    code,
                    codeVerifier,
                    properties.normalizedResource(resource));
        } else if ("refresh_token".equals(grantType)) {
            tokenResponse = tokenService.refresh(refreshToken, clientId, properties.normalizedResource(resource));
        } else {
            throw new BizException(ResultCode.PARAM_INVALID, "unsupported grant_type");
        }
        return tokenResponse(tokenResponse);
    }

    @PostMapping(value = "/oauth/revoke", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> revoke(@RequestParam("token") String token) {
        tokenService.revoke(token);
        return ResponseEntity.ok().build();
    }

    private static Map<String, Object> tokenResponse(OAuthTokenResponse response) {
        return Map.of(
                "access_token", response.getAccessToken(),
                "token_type", response.getTokenType(),
                "expires_in", response.getExpiresIn(),
                "refresh_token", response.getRefreshToken(),
                "scope", response.getScope(),
                "resource", response.getResource()
        );
    }

    private static Set<String> parseScopes(String scope) {
        if (scope == null || scope.isBlank()) {
            return Set.of();
        }
        return new LinkedHashSet<>(Arrays.stream(scope.split("\\s+")).filter(s -> !s.isBlank()).toList());
    }

    private McpProperties.Client requireClient(String clientId) {
        try {
            return properties.requireClient(clientId);
        } catch (IllegalArgumentException e) {
            throw new BizException(ResultCode.ACCESS_DENIED, e.getMessage());
        }
    }
}
