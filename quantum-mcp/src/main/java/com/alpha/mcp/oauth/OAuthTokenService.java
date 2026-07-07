package com.alpha.mcp.oauth;

import com.alpha.cache.util.CacheClient;
import com.alpha.framework.entity.LoginUser;
import com.alpha.framework.enums.ResultCode;
import com.alpha.framework.exception.BizException;
import com.alpha.mcp.config.McpProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OAuthTokenService {

    private static final String CODE_PREFIX = "quantum:oauth:code:";
    private static final String ACCESS_PREFIX = "quantum:oauth:access:";
    private static final String REFRESH_PREFIX = "quantum:oauth:refresh:";
    private static final String USED_REFRESH_PREFIX = "quantum:oauth:refresh-used:";
    private static final String USER_PREFIX = "quantum:oauth:user:";

    private final CacheClient cacheClient;
    private final McpProperties properties;

    public String createAuthorizationCode(AuthorizationRequest request, LoginUser loginUser) {
        requireEnabled();
        if (loginUser == null || loginUser.getUserId() == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "login user is required");
        }
        McpProperties.Client client = validateClient(request.clientId(), request.redirectUri());
        String resource = validateResource(request.resource());
        validatePkceMethod(request.codeChallengeMethod());
        Set<String> scopes = normalizeScopes(request.scopes(), client);

        String codeId = randomToken();
        OAuthAuthorizationCode code = new OAuthAuthorizationCode()
                .setCodeId(codeId)
                .setUserId(loginUser.getUserId())
                .setLoginUser(loginUser)
                .setClientId(request.clientId())
                .setRedirectUri(request.redirectUri())
                .setCodeChallenge(request.codeChallenge())
                .setCodeChallengeMethod(PkceUtil.S256)
                .setResource(resource)
                .setScopes(scopes)
                .setNonce(request.nonce())
                .setIssuedAt(LocalDateTime.now());
        cacheClient.set(codeKey(codeId), code, properties.getAuthorizationCodeTtl());
        return codeId;
    }

    public OAuthTokenResponse exchangeAuthorizationCode(
            String clientId,
            String redirectUri,
            String code,
            String codeVerifier,
            String resource) {
        requireEnabled();
        validateClient(clientId, redirectUri);
        String normalizedResource = validateResource(resource);
        OAuthAuthorizationCode storedCode = cacheClient.getAndDelete(codeKey(code));
        if (storedCode == null) {
            throw new BizException(ResultCode.TOKEN_INVALID, "authorization code is invalid or already used");
        }
        if (!clientId.equals(storedCode.getClientId())
                || !redirectUri.equals(storedCode.getRedirectUri())
                || !normalizedResource.equals(storedCode.getResource())) {
            throw new BizException(ResultCode.TOKEN_INVALID, "authorization code binding mismatch");
        }
        if (!PkceUtil.matchesS256(codeVerifier, storedCode.getCodeChallenge())) {
            throw new BizException(ResultCode.TOKEN_INVALID, "PKCE verification failed");
        }

        LoginUser loginUser = storedCode.getLoginUser();
        return issueTokens(loginUser, clientId, normalizedResource, storedCode.getScopes());
    }

    public OAuthTokenResponse refresh(String refreshToken, String clientId, String resource) {
        requireEnabled();
        String normalizedResource = validateResource(resource);
        OAuthRefreshToken oldRefreshToken = cacheClient.getAndDelete(refreshKey(refreshToken));
        if (oldRefreshToken == null) {
            OAuthRefreshToken used = cacheClient.get(usedRefreshKey(refreshToken));
            if (used != null) {
                revokeClientTokens(used.getUserId(), used.getClientId());
            }
            throw new BizException(ResultCode.TOKEN_INVALID, "refresh token is invalid or already used");
        }
        if (!clientId.equals(oldRefreshToken.getClientId()) || !normalizedResource.equals(oldRefreshToken.getResource())) {
            throw new BizException(ResultCode.TOKEN_INVALID, "refresh token binding mismatch");
        }

        cacheClient.set(usedRefreshKey(refreshToken), oldRefreshToken, properties.getRefreshTokenTtl());
        revokeAccessToken(oldRefreshToken.getAccessTokenId());
        return issueTokens(
                oldRefreshToken.getLoginUser(),
                oldRefreshToken.getClientId(),
                oldRefreshToken.getResource(),
                oldRefreshToken.getScopes()
        );
    }

    public OAuthAccessToken validateAccessToken(String accessToken, String resource) {
        if (accessToken == null || accessToken.isBlank()) {
            return null;
        }
        OAuthAccessToken token = cacheClient.get(accessKey(accessToken));
        if (token == null) {
            return null;
        }
        String normalizedResource = properties.normalizedResource(resource);
        if (!normalizedResource.equals(token.getResource())) {
            return null;
        }
        LoginUser loginUser = token.getLoginUser();
        if (loginUser == null || !loginUser.isEnabled()) {
            return null;
        }
        return token;
    }

    public void revoke(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        OAuthAccessToken accessToken = cacheClient.get(accessKey(token));
        if (accessToken != null) {
            revokeAccessToken(token);
            return;
        }
        OAuthRefreshToken refreshToken = cacheClient.get(refreshKey(token));
        if (refreshToken != null) {
            cacheClient.delete(refreshKey(token));
            revokeAccessToken(refreshToken.getAccessTokenId());
        }
    }

    public void revokeClientTokens(Long userId, String clientId) {
        if (userId == null || clientId == null) {
            return;
        }
        Set<String> tokenIds = cacheClient.sMembers(userKey(userId));
        if (tokenIds == null || tokenIds.isEmpty()) {
            return;
        }
        for (String tokenId : tokenIds) {
            OAuthAccessToken accessToken = cacheClient.get(accessKey(tokenId));
            if (accessToken != null && clientId.equals(accessToken.getClientId())) {
                revokeAccessToken(tokenId);
            }
        }
    }

    private OAuthTokenResponse issueTokens(LoginUser loginUser, String clientId, String resource, Set<String> scopes) {
        if (loginUser == null || loginUser.getUserId() == null || !loginUser.isEnabled()) {
            throw new BizException(ResultCode.UNAUTHORIZED, "enabled login user is required");
        }
        String accessTokenId = randomToken();
        String refreshTokenId = randomToken();

        OAuthAccessToken accessToken = new OAuthAccessToken()
                .setTokenId(accessTokenId)
                .setRefreshTokenId(refreshTokenId)
                .setLoginUser(loginUser)
                .setClientId(clientId)
                .setResource(resource)
                .setScopes(scopes)
                .setIssuedAt(LocalDateTime.now());
        OAuthRefreshToken refreshToken = new OAuthRefreshToken()
                .setRefreshTokenId(refreshTokenId)
                .setAccessTokenId(accessTokenId)
                .setLoginUser(loginUser)
                .setUserId(loginUser.getUserId())
                .setClientId(clientId)
                .setResource(resource)
                .setScopes(scopes)
                .setIssuedAt(LocalDateTime.now());

        cacheClient.set(accessKey(accessTokenId), accessToken, properties.getAccessTokenTtl());
        cacheClient.set(refreshKey(refreshTokenId), refreshToken, properties.getRefreshTokenTtl());
        cacheClient.sAdd(userKey(loginUser.getUserId()), accessTokenId);
        cacheClient.expire(userKey(loginUser.getUserId()), properties.getRefreshTokenTtl());

        return new OAuthTokenResponse()
                .setAccessToken(accessTokenId)
                .setExpiresIn(properties.getAccessTokenTtl().toSeconds())
                .setRefreshToken(refreshTokenId)
                .setScope(String.join(" ", scopes))
                .setResource(resource);
    }

    private void revokeAccessToken(String accessTokenId) {
        OAuthAccessToken accessToken = cacheClient.get(accessKey(accessTokenId));
        if (accessToken == null) {
            return;
        }
        cacheClient.delete(accessKey(accessTokenId));
        if (accessToken.getRefreshTokenId() != null) {
            cacheClient.delete(refreshKey(accessToken.getRefreshTokenId()));
        }
        LoginUser loginUser = accessToken.getLoginUser();
        if (loginUser != null && loginUser.getUserId() != null) {
            cacheClient.sRemove(userKey(loginUser.getUserId()), accessTokenId);
        }
    }

    private McpProperties.Client validateClient(String clientId, String redirectUri) {
        if (clientId == null || clientId.isBlank()) {
            throw new BizException(ResultCode.PARAM_MISSING, "client_id is required");
        }
        McpProperties.Client client;
        try {
            client = properties.requireClient(clientId);
        } catch (IllegalArgumentException e) {
            throw new BizException(ResultCode.ACCESS_DENIED, e.getMessage());
        }
        if (redirectUri == null || !client.getRedirectUris().contains(redirectUri)) {
            throw new BizException(ResultCode.ACCESS_DENIED, "redirect_uri is not allowed");
        }
        return client;
    }

    private String validateResource(String resource) {
        String normalizedResource = properties.normalizedResource(resource);
        if (!properties.getResource().equals(normalizedResource)) {
            throw new BizException(ResultCode.ACCESS_DENIED, "resource is not allowed");
        }
        return normalizedResource;
    }

    private void validatePkceMethod(String codeChallengeMethod) {
        if (!PkceUtil.S256.equals(codeChallengeMethod)) {
            throw new BizException(ResultCode.PARAM_INVALID, "only PKCE S256 is supported");
        }
    }

    private Set<String> normalizeScopes(Set<String> requestedScopes, McpProperties.Client client) {
        Set<String> scopes = requestedScopes == null || requestedScopes.isEmpty()
                ? new LinkedHashSet<>(properties.getDefaultScopes())
                : new LinkedHashSet<>(requestedScopes);
        if (!client.getScopes().containsAll(scopes)) {
            throw new BizException(ResultCode.ACCESS_DENIED, "scope is not allowed");
        }
        return scopes;
    }

    private void requireEnabled() {
        if (!properties.isEnabled()) {
            throw new BizException(ResultCode.SERVICE_UNAVAILABLE, "MCP is disabled");
        }
    }

    private static String randomToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String codeKey(String codeId) {
        return CODE_PREFIX + codeId;
    }

    private static String accessKey(String tokenId) {
        return ACCESS_PREFIX + tokenId;
    }

    private static String refreshKey(String refreshTokenId) {
        return REFRESH_PREFIX + refreshTokenId;
    }

    private static String usedRefreshKey(String refreshTokenId) {
        return USED_REFRESH_PREFIX + refreshTokenId;
    }

    private static String userKey(Long userId) {
        return USER_PREFIX + userId;
    }

    public record AuthorizationRequest(
            String clientId,
            String redirectUri,
            String codeChallenge,
            String codeChallengeMethod,
            String resource,
            Set<String> scopes,
            String nonce
    ) {
    }
}
