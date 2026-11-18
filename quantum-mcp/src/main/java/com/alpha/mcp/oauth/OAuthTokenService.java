package com.alpha.mcp.oauth;

import com.alpha.cache.util.CacheClient;
import com.alpha.framework.entity.LoginUser;
import com.alpha.framework.enums.ResultCode;
import com.alpha.framework.exception.BizException;
import com.alpha.mcp.config.McpProperties;
import com.alpha.security.token.SessionGeneration;
import com.alpha.system.security.ForceLogoutEvent;
import com.alpha.system.service.impl.UserDetailsServiceImpl;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.event.TransactionalEventListener;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
public class OAuthTokenService {

    private static final String CODE_PREFIX = "quantum:oauth:code:";
    private static final String ACCESS_PREFIX = "quantum:oauth:access:";
    private static final String REFRESH_PREFIX = "quantum:oauth:refresh:";
    private static final String USED_REFRESH_PREFIX = "quantum:oauth:refresh-used:";
    private static final String USER_PREFIX = "quantum:oauth:user-refresh:";
    private final CacheClient cacheClient;
    private final McpProperties properties;
    private final UserDetailsServiceImpl userDetails;
    private final SessionGeneration generations;

    public String createAuthorizationCode(AuthorizationRequest request, LoginUser loginUser) {
        requireEnabled();
        if (loginUser == null || !generations.matches(loginUser.getUserId(), loginUser.getSessionGeneration())) {
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
                .setSessionVersion(loginUser.getSessionGeneration())
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
        if (codeVerifier == null || codeVerifier.isBlank()) {
            throw new BizException(ResultCode.PARAM_INVALID, "PKCE verifier is required");
        }
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

        requireCurrentUser(storedCode.getUserId(), storedCode.getSessionVersion());
        return issueTokens(storedCode.getUserId(), storedCode.getSessionVersion(),
                clientId, normalizedResource, storedCode.getScopes());
    }

    public OAuthTokenResponse refresh(String refreshToken, String clientId, String resource) {
        requireEnabled();
        String normalizedResource = validateResource(resource);
        String key = refreshKey(refreshToken);
        OAuthRefreshToken candidate = cacheClient.get(key);
        if (candidate != null) {
            if (!clientId.equals(candidate.getClientId()) || !normalizedResource.equals(candidate.getResource())) {
                throw new BizException(ResultCode.TOKEN_INVALID, "refresh token binding mismatch");
            }
            requireCurrentUser(candidate.getUserId(), candidate.getSessionVersion());
        }

        // Keep the token usable if the user store fails, while allowing only one rotation.
        OAuthRefreshToken oldRefreshToken = candidate == null ? null : cacheClient.getAndDelete(key);
        if (oldRefreshToken == null) {
            OAuthRefreshToken used = cacheClient.get(usedRefreshKey(refreshToken));
            if (used != null && generations.matches(used.getUserId(), used.getSessionVersion())) {
                revokeSessions(used.getUserId(), token -> used.getClientId().equals(token.getClientId())
                        && used.getSessionVersion().equals(token.getSessionVersion()));
            }
            throw new BizException(ResultCode.TOKEN_INVALID, "refresh token is invalid or already used");
        }
        cacheClient.set(usedRefreshKey(refreshToken), oldRefreshToken, properties.getRefreshTokenTtl());
        removeSession(oldRefreshToken);
        return issueTokens(
                oldRefreshToken.getUserId(),
                oldRefreshToken.getSessionVersion(),
                oldRefreshToken.getClientId(),
                oldRefreshToken.getResource(),
                oldRefreshToken.getScopes()
        );
    }

    public ValidatedAccessToken validateAccessToken(String accessToken, String resource) {
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
        LoginUser loginUser = currentUser(token.getUserId(), token.getSessionVersion());
        return loginUser == null ? null : new ValidatedAccessToken(token, loginUser);
    }

    public void revoke(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        OAuthAccessToken accessToken = cacheClient.getAndDelete(accessKey(token));
        String refreshId = accessToken == null ? token : accessToken.getRefreshTokenId();
        OAuthRefreshToken refresh = cacheClient.getAndDelete(refreshKey(refreshId));
        if (refresh != null) {
            removeSession(refresh);
        }
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void onForceLogout(ForceLogoutEvent event) {
        if (properties.isEnabled()) {
            event.userIds().forEach(userId -> revokeSessions(userId,
                    token -> !generations.matches(userId, token.getSessionVersion())));
        }
    }

    private void revokeSessions(Long userId, Predicate<OAuthRefreshToken> shouldRevoke) {
        for (String refreshId : cacheClient.<String>sMembers(userKey(userId))) {
            OAuthRefreshToken token = cacheClient.get(refreshKey(refreshId));
            if (token == null) {
                cacheClient.sRemove(userKey(userId), refreshId);
            } else if (shouldRevoke.test(token)) {
                cacheClient.delete(refreshKey(refreshId));
                removeSession(token);
            }
        }
    }

    private OAuthTokenResponse issueTokens(Long userId, String sessionVersion, String clientId,
                                           String resource, Set<String> scopes) {
        String accessTokenId = randomToken();
        String refreshTokenId = randomToken();

        OAuthAccessToken accessToken = new OAuthAccessToken()
                .setTokenId(accessTokenId)
                .setRefreshTokenId(refreshTokenId)
                .setUserId(userId)
                .setSessionVersion(sessionVersion)
                .setClientId(clientId)
                .setResource(resource)
                .setScopes(scopes)
                .setIssuedAt(LocalDateTime.now());
        OAuthRefreshToken refreshToken = new OAuthRefreshToken()
                .setRefreshTokenId(refreshTokenId)
                .setAccessTokenId(accessTokenId)
                .setSessionVersion(sessionVersion)
                .setUserId(userId)
                .setClientId(clientId)
                .setResource(resource)
                .setScopes(scopes)
                .setIssuedAt(LocalDateTime.now());

        cacheClient.set(accessKey(accessTokenId), accessToken, properties.getAccessTokenTtl());
        cacheClient.set(refreshKey(refreshTokenId), refreshToken, properties.getRefreshTokenTtl());
        cacheClient.sAdd(userKey(userId), refreshTokenId);
        cacheClient.expire(userKey(userId), properties.getRefreshTokenTtl());
        if (!generations.matches(userId, sessionVersion)) {
            cacheClient.delete(refreshKey(refreshTokenId));
            removeSession(refreshToken);
            throw new BizException(ResultCode.TOKEN_INVALID, "OAuth session is no longer valid");
        }

        return new OAuthTokenResponse()
                .setAccessToken(accessTokenId)
                .setExpiresIn(properties.getAccessTokenTtl().toSeconds())
                .setRefreshToken(refreshTokenId)
                .setScope(String.join(" ", scopes))
                .setResource(resource);
    }

    private void removeSession(OAuthRefreshToken refresh) {
        cacheClient.delete(accessKey(refresh.getAccessTokenId()));
        cacheClient.sRemove(userKey(refresh.getUserId()), refresh.getRefreshTokenId());
    }

    private void requireCurrentUser(Long userId, String sessionVersion) {
        if (currentUser(userId, sessionVersion) == null) {
            throw new BizException(ResultCode.TOKEN_INVALID, "OAuth session is no longer valid");
        }
    }

    private LoginUser currentUser(Long userId, String sessionVersion) {
        if (!generations.matches(userId, sessionVersion)) {
            return null;
        }
        try {
            LoginUser user = userDetails.loadUserById(userId);
            return user.isEnabled() && generations.matches(userId, sessionVersion) ? user : null;
        } catch (UsernameNotFoundException e) {
            return null;
        }
    }

    public record ValidatedAccessToken(OAuthAccessToken token, LoginUser user) {
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
