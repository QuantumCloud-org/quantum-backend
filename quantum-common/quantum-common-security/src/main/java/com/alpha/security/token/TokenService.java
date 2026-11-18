package com.alpha.security.token;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alpha.cache.util.CacheClient;
import com.alpha.framework.constant.CommonConstants;
import com.alpha.framework.entity.LoginUser;
import com.alpha.framework.enums.ResultCode;
import com.alpha.framework.exception.BizException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Token 服务 - JWT + 缓存混合方案（支持 Redis 和 Caffeine 本地缓存）
 */
@Slf4j
@Service
public class TokenService {

    private static final String ACCESS_TOKEN_PREFIX = CommonConstants.REDIS_TOKEN_PREFIX + "access:";
    private static final String SESSION_PREFIX = CommonConstants.REDIS_TOKEN_PREFIX + "web-session:";
    private static final String ACCESS_SESSION_PREFIX = CommonConstants.REDIS_TOKEN_PREFIX + "access-session:";
    private static final String ACCESS = "access";
    private static final String REFRESH = "refresh";
    private static final String ONLINE_INDEX = "online:tokens:expires";
    private static final String USER_TOKEN_PREFIX = CommonConstants.USER_TOKEN_PREFIX + "live:";

    private final CacheClient cacheClient;
    private final TokenProperties tokenProperties;
    private final UserDetailsService userDetailsService;
    private final SessionGeneration generations;
    private final MACSigner jwtSigner;
    private final MACVerifier jwtVerifier;

    public TokenService(CacheClient cacheClient, TokenProperties tokenProperties, UserDetailsService userDetailsService,
                        SessionGeneration generations) {
        this.cacheClient = cacheClient;
        this.tokenProperties = tokenProperties;
        this.userDetailsService = userDetailsService;
        this.generations = generations;
        tokenProperties.validate();
        byte[] key = tokenProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        try {
            this.jwtSigner = new MACSigner(key);
            this.jwtVerifier = new MACVerifier(key);
        } catch (JOSEException e) {
            throw new IllegalStateException("配置的 JWT 签名密钥无效", e);
        }
    }

    /** Creates a Web session bound to the generation captured before authentication. */
    public TokenInfo createToken(LoginUser user, String generation, String deviceId, String clientIp,
                                 String userAgent, boolean rememberMe) throws JOSEException {
        var identity = new SessionIdentity(user.getUserId(), IdUtil.fastSimpleUUID(), generation);
        var client = new ClientInfo(normalizeDevice(deviceId), clientIp, userAgent);
        TokenInfo token = registerAccess(user, identity, client, rememberMe);
        if (!generations.matches(identity.userId(), generation)) {
            throw new BizException(ResultCode.SESSION_TRANSITION);
        }
        cacheClient.set(sessionKey(identity.sessionId()), token.getRefreshTokenId(), refreshTtl());
        if (tokenProperties.isSingleDevice()) {
            kickOtherDevices(identity);
        }
        return token;
    }

    /** Verifies the signed access token and both its user and its Web session. */
    public LoginUser validateToken(String token) throws ParseException, JOSEException {
        JWTClaimsSet claims = readClaims(token, ACCESS);
        if (claims == null || !hasValidExpiration(claims)) {
            return null;
        }
        SessionIdentity identity = readIdentity(claims);
        String accessKey = buildAccessTokenKey(claims.getJWTID());
        String webKey = sessionKey(identity.sessionId());
        String generationKey = SessionGeneration.key(identity.userId());
        Map<String, Object> state = cacheClient.getAll(List.of(accessKey, webKey, generationKey));
        return state.get(accessKey) instanceof LoginUser user && user.isEnabled()
                && state.containsKey(webKey) && identity.generation().equals(state.get(generationKey)) ? user : null;
    }

    /** Stages the successor before atomically consuming the old refresh credential. */
    public TokenInfo refreshToken(String refreshToken, String deviceId, String clientIp, String userAgent)
            throws ParseException, JOSEException {
        JWTClaimsSet claims;
        try {
            claims = readClaims(refreshToken, REFRESH);
        } catch (ParseException e) {
            log.debug("忽略无法验证的刷新令牌");
            return null;
        }
        var client = new ClientInfo(normalizeDevice(deviceId), clientIp, userAgent);
        if (claims == null || !matchesClient(claims, client)) {
            return null;
        }
        SessionIdentity identity = readIdentity(claims);
        if (!generations.matches(identity.userId(), identity.generation())) {
            return null;
        }
        String key = sessionKey(identity.sessionId());
        String oldId = claims.getJWTID();
        // A signed predecessor must leave the newer cookie intact even after the predecessor expires.
        if (!isCurrentRefresh(key, oldId) || !hasValidExpiration(claims)) {
            return null;
        }
        LoginUser user;
        try {
            user = loadLoginUser(claims.getStringClaim("username"));
        } catch (UsernameNotFoundException e) {
            return null;
        }
        if (!user.isEnabled() || !identity.userId().equals(user.getUserId())) {
            return null;
        }

        TokenInfo token = registerAccess(user, identity, client, Boolean.TRUE.equals(claims.getBooleanClaim("rememberMe")));
        // The signed generation stays authoritative even when revocation races with the atomic rotation.
        if (!generations.matches(identity.userId(), identity.generation())) {
            return null;
        }
        if (cacheClient.compareAndSet(key, oldId, token.getRefreshTokenId(), refreshTtl())) {
            return token;
        }
        isCurrentRefresh(key, oldId);
        return null;
    }

    private boolean isCurrentRefresh(String key, String refreshId) {
        String current = cacheClient.get(key);
        if (current == null) {
            return false;
        }
        if (!current.equals(refreshId)) {
            throw new BizException(ResultCode.DUPLICATE_REQUEST);
        }
        return true;
    }

    private TokenInfo registerAccess(LoginUser user, SessionIdentity identity, ClientInfo client, boolean rememberMe)
            throws JOSEException {
        String tokenId = IdUtil.fastSimpleUUID();
        String refreshId = IdUtil.fastSimpleUUID();
        String access = generateJwtToken(tokenId, identity, user.getUsername(), client, ACCESS, false, accessTtl());
        String refresh = generateJwtToken(refreshId, identity, user.getUsername(), client, REFRESH, rememberMe, refreshTtl());
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiry = now.plus(accessTtl());
        user.setTokenId(tokenId).setRefreshTokenId(refreshId).setWebSessionId(identity.sessionId())
                .setSessionGeneration(identity.generation()).setLoginTime(now).setExpireTime(expiry).setPassword(null);

        String accessKey = buildAccessTokenKey(tokenId);
        long nowMillis = System.currentTimeMillis();
        cacheClient.set(accessKey, user, accessTtl());
        cacheClient.set(accessSessionKey(tokenId), identity.sessionId(), refreshTtl());
        String userKey = USER_TOKEN_PREFIX + user.getUserId();
        cacheClient.zAdd(userKey, tokenId, nowMillis + refreshTtl().toMillis());
        cacheClient.zRemoveThrough(userKey, nowMillis);
        cacheClient.expire(userKey, refreshTtl());
        cacheClient.zAdd(ONLINE_INDEX, accessKey, nowMillis + accessTtl().toMillis());

        return new TokenInfo().setAccessToken(access).setRefreshToken(refresh).setSessionId(identity.sessionId())
                .setExpiresIn((int) accessTtl().toSeconds()).setExpireTime(expiry).setRememberMe(rememberMe)
                .setUserId(user.getUserId()).setUsername(user.getUsername()).setTokenId(tokenId)
                .setRefreshTokenId(refreshId).setDeviceId(client.deviceId()).setClientIp(client.ip())
                .setUserAgent(client.userAgent()).setCreatedAt(System.currentTimeMillis()).setRefreshCount(0);
    }

    /** Revokes the requested Web session and reports whether its browser cookie can be cleared. */
    public boolean logout(String accessToken, String refreshToken) throws JOSEException {
        JWTClaimsSet access = readLogoutClaims(accessToken, ACCESS);
        JWTClaimsSet refresh = readLogoutClaims(refreshToken, REFRESH);
        String accessSession = access == null ? null : (String) access.getClaim("sessionId");
        String refreshSession = refresh == null ? null : (String) refresh.getClaim("sessionId");
        if (access != null && hasValidExpiration(access)) {
            revokeWebSession(accessSession);
        }
        if (accessSession != null && refreshSession != null && !accessSession.equals(refreshSession)) {
            return false;
        }
        if (refresh != null && hasValidExpiration(refresh)) {
            String key = sessionKey(refreshSession);
            if (refresh.getJWTID().equals(cacheClient.<String>get(key))) {
                cacheClient.delete(key);
            }
        }
        return true;
    }

    private JWTClaimsSet readLogoutClaims(String token, String kind) throws JOSEException {
        try {
            return readClaims(token, kind);
        } catch (ParseException e) {
            log.debug("忽略无法验证的登出令牌");
            return null;
        }
    }

    /** Revokes the Web session addressed by an online management row. */
    public void logoutByTokenId(String tokenId) {
        if (StrUtil.isNotBlank(tokenId)) {
            String sessionId = cacheClient.get(accessSessionKey(tokenId));
            if (sessionId == null) {
                LoginUser user = cacheClient.get(buildAccessTokenKey(tokenId));
                sessionId = user == null ? null : user.getWebSessionId();
            }
            revokeWebSession(sessionId);
        }
    }

    /** Invalidates both Web and MCP generations independently of best-effort indexes. */
    public void kickOut(Long userId) {
        generations.revoke(userId);
        log.info("强制下线用户 | UserId: {}", userId);
    }

    /**
     * 刷新用户会话缓存（不踢下线）
     */
    public void refreshUserCache(Long userId) {
        if (userId == null) {
            return;
        }
        Collection<String> tokens = indexedTokenIds(userId);
        if (tokens.isEmpty()) {
            return;
        }
        Map<String, LoginUser> sessions = cacheClient.getAll(tokens.stream().map(TokenService::buildAccessTokenKey).toList());
        if (sessions.isEmpty()) {
            return;
        }
        LoginUser fresh;
        try {
            fresh = loadLoginUser(sessions.values().iterator().next().getUsername());
        } catch (RuntimeException e) {
            log.warn("刷新用户权限失败 | userId={}", userId, e);
            return;
        }
        if (!userId.equals(fresh.getUserId()) || !fresh.isEnabled()) {
            kickOut(userId);
            return;
        }
        sessions.forEach((key, oldUser) -> refreshCachedSession(key, oldUser, fresh));
    }

    private void refreshCachedSession(String key, LoginUser oldUser, LoginUser fresh) {
        long ttl = cacheClient.getExpire(key);
        if (ttl <= 0) {
            return;
        }
        var copy = new LoginUser();
        BeanUtils.copyProperties(fresh, copy);
        copy.setTokenId(oldUser.getTokenId()).setRefreshTokenId(oldUser.getRefreshTokenId())
                .setWebSessionId(oldUser.getWebSessionId()).setSessionGeneration(oldUser.getSessionGeneration())
                .setLoginTime(oldUser.getLoginTime()).setExpireTime(oldUser.getExpireTime())
                .setLoginIp(oldUser.getLoginIp()).setLoginLocation(oldUser.getLoginLocation())
                .setBrowser(oldUser.getBrowser()).setOs(oldUser.getOs()).setPassword(null);
        cacheClient.replaceIfPresent(key, copy, Duration.ofSeconds(ttl));
    }

    public Map<String, LoginUser> getOnlineSessions() {
        pruneOnlineSessions();
        var keys = cacheClient.<String>zRangeByScore(ONLINE_INDEX, System.currentTimeMillis(), Double.POSITIVE_INFINITY);
        var sessions = cacheClient.<LoginUser>getAll(keys);
        Map<String, String> refreshIds = cacheClient.getAll(sessions.values().stream()
                .map(user -> sessionKey(user.getWebSessionId())).distinct().toList());
        Map<String, String> current = generations.current(sessions.values().stream().map(LoginUser::getUserId).distinct().toList());
        return sessions.entrySet().stream().filter(entry -> isOnline(entry.getValue(), refreshIds, current))
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static boolean isOnline(LoginUser user, Map<String, String> refreshIds, Map<String, String> current) {
        return user.isEnabled() && user.getWebSessionId() != null && user.getSessionGeneration() != null
                && user.getRefreshTokenId().equals(refreshIds.get(sessionKey(user.getWebSessionId())))
                && user.getSessionGeneration().equals(current.get(SessionGeneration.key(user.getUserId())));
    }

    public long getOnlineCount() {
        return getOnlineSessions().size();
    }

    @Scheduled(fixedDelay = 60_000)
    public void pruneOnlineSessions() {
        cacheClient.zRemoveThrough(ONLINE_INDEX, System.currentTimeMillis());
    }

    public int getRefreshTokenExpireSeconds() {
        return (int) refreshTtl().toSeconds();
    }

    private LoginUser loadLoginUser(String username) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (!(userDetails instanceof LoginUser loginUser)) {
            throw new IllegalStateException("用户信息加载失败");
        }
        return loginUser;
    }

    private String generateJwtToken(String tokenId, SessionIdentity identity, String username, ClientInfo client,
                                    String kind, boolean rememberMe, Duration ttl) throws JOSEException {
        Date now = new Date();
        var claims = new JWTClaimsSet.Builder()
                .jwtID(tokenId)
                .subject(identity.userId().toString())
                .issueTime(now)
                .expirationTime(new Date(now.getTime() + ttl.toMillis()))
                .claim("username", username)
                .claim("sessionId", identity.sessionId())
                .claim("generation", identity.generation())
                .claim("kind", kind);
        if (REFRESH.equals(kind)) {
            claims.claim("rememberMe", rememberMe)
                    .claim("deviceId", client.deviceId())
                    .claim("clientIp", client.ip())
                    .claim("userAgent", client.userAgent());
        }
        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS512), claims.build());
        signedJWT.sign(jwtSigner);
        return signedJWT.serialize();
    }

    private JWTClaimsSet readClaims(String token, String kind) throws ParseException, JOSEException {
        if (StrUtil.isBlank(token)) {
            return null;
        }
        JWTClaimsSet claims = parseVerifiedClaims(token);
        if (claims == null || StrUtil.isBlank(claims.getJWTID()) || claims.getExpirationTime() == null
                || !kind.equals(claims.getStringClaim("kind")) || StrUtil.isBlank(claims.getStringClaim("sessionId"))
                || StrUtil.isBlank(claims.getStringClaim("generation"))) {
            return null;
        }
        return claims;
    }

    private boolean matchesClient(JWTClaimsSet claims, ClientInfo client) throws ParseException {
        return !tokenProperties.isVerifyClientInfo()
                || Objects.equals(claims.getStringClaim("deviceId"), client.deviceId())
                && Objects.equals(claims.getStringClaim("clientIp"), client.ip())
                && Objects.equals(claims.getStringClaim("userAgent"), client.userAgent());
    }

    private static SessionIdentity readIdentity(JWTClaimsSet claims) throws ParseException {
        return new SessionIdentity(Long.valueOf(claims.getSubject()), claims.getStringClaim("sessionId"),
                claims.getStringClaim("generation"));
    }

    private JWTClaimsSet parseVerifiedClaims(String token) throws ParseException, JOSEException {
        SignedJWT signedJWT = SignedJWT.parse(token);
        if (!JWSAlgorithm.HS512.equals(signedJWT.getHeader().getAlgorithm())) {
            return null;
        }
        return signedJWT.verify(jwtVerifier) ? signedJWT.getJWTClaimsSet() : null;
    }

    private static boolean hasValidExpiration(JWTClaimsSet claims) {
        Date expiration = claims.getExpirationTime();
        return expiration != null && expiration.after(new Date());
    }


    private void kickOtherDevices(SessionIdentity identity) {
        var tokenIds = indexedTokenIds(identity.userId());
        var sessions = cacheClient.<String>getAll(tokenIds.stream().map(TokenService::accessSessionKey).toList());
        sessions.values().stream().distinct().filter(id -> !identity.sessionId().equals(id)).forEach(this::revokeWebSession);
    }

    private Collection<String> indexedTokenIds(Long userId) {
        return cacheClient.zRangeByScore(USER_TOKEN_PREFIX + userId, System.currentTimeMillis(), Double.POSITIVE_INFINITY);
    }

    private void revokeWebSession(String sessionId) {
        if (sessionId != null) {
            cacheClient.delete(sessionKey(sessionId));
        }
    }

    /** Identifies a Web access session within the cache's key space. */
    public static String tokenIdOf(String key) {
        return key != null && key.startsWith(ACCESS_TOKEN_PREFIX) ? key.substring(ACCESS_TOKEN_PREFIX.length()) : null;
    }

    private static String buildAccessTokenKey(String tokenId) {
        return ACCESS_TOKEN_PREFIX + tokenId;
    }

    private static String accessSessionKey(String tokenId) {
        return ACCESS_SESSION_PREFIX + tokenId;
    }

    private static String sessionKey(String sessionId) {
        return SESSION_PREFIX + sessionId;
    }

    private Duration accessTtl() {
        return Duration.ofMinutes(tokenProperties.getAccessTokenExpire());
    }

    private Duration refreshTtl() {
        return Duration.ofMinutes(tokenProperties.getRefreshTokenExpire());
    }

    private static String normalizeDevice(String deviceId) {
        return StrUtil.blankToDefault(deviceId, "default");
    }

    private record SessionIdentity(Long userId, String sessionId, String generation) {
    }

    private record ClientInfo(String deviceId, String ip, String userAgent) {
    }
}
