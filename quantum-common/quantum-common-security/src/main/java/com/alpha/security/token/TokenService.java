package com.alpha.security.token;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alpha.cache.util.RedisUtil;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Objects;
import java.util.Set;

/**
 * Token 服务 - JWT + Redis 混合方案
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private static final String ACCESS_TOKEN_PREFIX = CommonConstants.REDIS_TOKEN_PREFIX + "access:";
    private static final String REFRESH_TOKEN_PREFIX = CommonConstants.REDIS_TOKEN_PREFIX + "refresh:";
    private static final String REFRESH_REMEMBER_PREFIX = CommonConstants.REDIS_TOKEN_PREFIX + "refresh-remember:";
    private static final String USER_TOKEN_PREFIX = CommonConstants.USER_TOKEN_PREFIX;

    private final RedisUtil redisUtil;
    private final TokenProperties tokenProperties;
    private final UserDetailsService userDetailsService;
    private final RedissonClient redissonClient;

    /**
     * 创建 Token
     */
    public TokenInfo createToken(LoginUser loginUser, String deviceId, String clientIp, String userAgent) throws JOSEException {
        return createToken(loginUser, deviceId, clientIp, userAgent, false);
    }

    /**
     * 创建 Token
     */
    public TokenInfo createToken(LoginUser loginUser, String deviceId, String clientIp, String userAgent, boolean rememberMe) throws JOSEException {
        String normalizedDeviceId = StrUtil.blankToDefault(deviceId, "default");
        String tokenId = IdUtil.fastSimpleUUID();
        String refreshTokenId = IdUtil.fastSimpleUUID();

        String accessToken = generateAccessToken(tokenId, loginUser.getUserId(), loginUser.getUsername(),
                normalizedDeviceId, clientIp, userAgent);
        String refreshToken = generateRefreshToken(refreshTokenId, loginUser.getUserId(), loginUser.getUsername(),
                normalizedDeviceId, clientIp, userAgent, rememberMe);

        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(tokenProperties.getAccessTokenExpire());

        loginUser.setTokenId(tokenId);
        loginUser.setRefreshTokenId(refreshTokenId);
        loginUser.setLoginTime(LocalDateTime.now());
        loginUser.setExpireTime(expireTime);

        long accessExpireMs = tokenProperties.getAccessTokenExpire() * 60 * 1000L;
        long refreshExpireMs = tokenProperties.getRefreshTokenExpire() * 60 * 1000L;

        String accessTokenKey = buildAccessTokenKey(tokenId);
        redisUtil.set(accessTokenKey, loginUser, Duration.ofMillis(accessExpireMs));
        // 存储 tokenId 作为值，refresh 时用于 revoke 旧 accessToken
        redisUtil.set(buildRefreshTokenKey(refreshTokenId), tokenId, Duration.ofMillis(refreshExpireMs));
        redisUtil.set(buildRefreshMapKey(tokenId), refreshTokenId, Duration.ofMillis(refreshExpireMs));
        redisUtil.set(buildUserMapKey(tokenId), String.valueOf(loginUser.getUserId()),
                Duration.ofMillis(Math.max(accessExpireMs, refreshExpireMs)));

        String userKey = USER_TOKEN_PREFIX + loginUser.getUserId();
        redisUtil.sAdd(userKey, tokenId);
        redisUtil.expire(userKey, Duration.ofMillis(Math.max(accessExpireMs, refreshExpireMs)));
        redisUtil.sAdd(CommonConstants.ONLINE_TOKENS_KEY, accessTokenKey);

        if (tokenProperties.isSingleDevice()) {
            kickOtherDevices(loginUser.getUserId(), tokenId);
        }

        saveRefreshRememberState(refreshTokenId, rememberMe);

        return new TokenInfo()
                .setAccessToken(accessToken)
                .setRefreshToken(refreshToken)
                .setUserId(loginUser.getUserId())
                .setUsername(loginUser.getUsername())
                .setTokenId(tokenId)
                .setDeviceId(normalizedDeviceId)
                .setRefreshTokenId(refreshTokenId)
                .setClientIp(clientIp)
                .setUserAgent(userAgent)
                .setCreatedAt(System.currentTimeMillis())
                .setRefreshCount(0)
                .setRememberMe(rememberMe)
                .setExpireTime(expireTime);
    }

    /**
     * 验证 Token 并返回用户信息
     */
    public LoginUser validateToken(String token) throws ParseException, JOSEException {
        if (StrUtil.isBlank(token)) {
            return null;
        }

        String tokenId = parseJwtTokenIdFromAccessToken(token);
        if (tokenId == null) {
            log.warn("【TokenService】JWT 签名验证失败或 TokenID 为空");
            return null;
        }

        String tokenKey = buildAccessTokenKey(tokenId);
        if (redisUtil.exists(CommonConstants.BLACKLIST_PREFIX + tokenId)) {
            log.warn("【TokenService】Token 在黑名单中 | TokenId: {}", tokenId);
            return null;
        }

        LoginUser loginUser = redisUtil.get(tokenKey);
        if (loginUser == null) {
            log.warn("【TokenService】Redis 中找不到 Token | TokenKey: {}", tokenKey);
            return null;
        }

        if (isJwtExpired(token)) {
            // JWT 已过期，返回 null 让 Filter 走 refreshToken 续期路径
            log.debug("【TokenService】JWT 已过期，需通过 RefreshToken 续期 | TokenId: {}", tokenId);
            return null;
        } else {
            renewIfNeeded(tokenKey, loginUser);
        }

        return loginUser;
    }

    /**
     * 刷新 Token
     */
    public TokenInfo refreshToken(String refreshToken, String deviceId, String clientIp, String userAgent) throws ParseException, JOSEException {
        if (StrUtil.isBlank(refreshToken)) {
            return null;
        }

        JWTClaimsSet claims;
        String refreshTokenId;
        try {
            SignedJWT signedJWT = SignedJWT.parse(refreshToken);
            MACVerifier verifier = new MACVerifier(tokenProperties.getSecret().getBytes());
            if (!signedJWT.verify(verifier)) {
                log.warn("RefreshToken JWT 签名验证失败");
                return null;
            }
            claims = signedJWT.getJWTClaimsSet();
            if (claims.getExpirationTime() != null && claims.getExpirationTime().before(new Date())) {
                log.debug("RefreshToken 已过期");
                return null;
            }
            refreshTokenId = claims.getJWTID();
        } catch (ParseException | JOSEException e) {
            log.warn("RefreshToken 解析或验签失败: {}", e.getMessage());
            return null;
        }

        if (refreshTokenId == null) {
            return null;
        }

        String normalizedDeviceId = StrUtil.blankToDefault(deviceId, "default");
        if (tokenProperties.isVerifyClientInfo()) {
            String bindDeviceId = claims.getStringClaim("deviceId");
            String bindIp = claims.getStringClaim("clientIp");
            String bindUa = claims.getStringClaim("userAgent");
            if (!Objects.equals(bindDeviceId, normalizedDeviceId)
                    || !Objects.equals(bindIp, clientIp)
                    || !Objects.equals(bindUa, userAgent)) {
                log.warn("refresh clientInfo mismatch, reject without consuming refresh | tokenId: {}", refreshTokenId);
                return null;
            }
        }

        String username = claims.getStringClaim("username");
        boolean rememberMe = loadRefreshRememberState(refreshTokenId);
        LoginUser loginUser;
        try {
            loginUser = loadLoginUser(username);
        } catch (Exception e) {
            log.warn("refresh loadLoginUser failed, keep old session intact", e);
            return null;
        }
        if (!loginUser.isEnabled()) {
            log.warn("用户已禁用，拒绝 refresh | username: {}", username);
            throw new BizException(ResultCode.ACCOUNT_DISABLED);
        }

        String refreshKey = buildRefreshTokenKey(refreshTokenId);
        RBucket<String> bucket = redissonClient.getBucket(refreshKey);
        String oldTokenId = bucket.getAndDelete();
        if (oldTokenId == null) {
            return null;
        }

        TokenInfo tokenInfo;
        try {
            tokenInfo = createToken(loginUser, normalizedDeviceId, clientIp, userAgent, rememberMe);
        } catch (JOSEException e) {
            log.error("createToken failed after refresh consume, session degraded to access-only", e);
            throw e;
        } catch (RuntimeException e) {
            log.error("createToken failed after refresh consume, session degraded to access-only", e);
            throw e;
        }

        revokeTokenById(oldTokenId);
        return tokenInfo;
    }

    /**
     * 登出
     */
    public void logout(String token) throws ParseException, JOSEException {
        if (StrUtil.isBlank(token)) {
            return;
        }

        String tokenId = parseJwtTokenIdFromAccessToken(token);
        if (tokenId != null) {
            revokeTokenById(tokenId);
        }
    }

    /**
     * 按 tokenId 登出
     */
    public void logoutByTokenId(String tokenId) {
        revokeTokenById(tokenId);
    }

    /**
     * 强制下线用户
     */
    public void kickOut(Long userId) {
        String userKey = USER_TOKEN_PREFIX + userId;
        Set<String> tokens = redisUtil.sMembers(userKey);
        if (tokens == null || tokens.isEmpty()) {
            return;
        }

        for (String tokenId : tokens) {
            revokeTokenById(tokenId);
        }
        redisUtil.delete(userKey);
        log.info("强制下线用户 | UserId: {} | Count: {}", userId, tokens.size());
    }

    /**
     * 刷新用户会话缓存（不踢下线）
     * <p>
     * 适用于资料、角色、部门、权限变更场景：保留原 JWT 与会话字段，
     * 仅重新加载 LoginUser 的权限/资料部分并回写 Redis，用户无感。
     */
    public void refreshUserCache(Long userId) {
        if (userId == null) return;
        String userKey = USER_TOKEN_PREFIX + userId;
        Set<String> tokens = redisUtil.sMembers(userKey);
        if (tokens == null || tokens.isEmpty()) return;

        int refreshed = 0;
        for (String tokenId : tokens) {
            String tokenKey = buildAccessTokenKey(tokenId);
            LoginUser oldUser = redisUtil.get(tokenKey);
            if (oldUser == null) continue;

            LoginUser fresh;
            try {
                fresh = loadLoginUser(oldUser.getUsername());
            } catch (Exception e) {
                log.warn("刷新用户缓存失败 | userId: {} | tokenId: {} | err: {}", userId, tokenId, e.getMessage());
                continue;
            }

            // 保留原有会话字段（JWT、登录上下文），覆盖资料/权限字段
            fresh.setTokenId(oldUser.getTokenId());
            fresh.setRefreshTokenId(oldUser.getRefreshTokenId());
            fresh.setLoginTime(oldUser.getLoginTime());
            fresh.setExpireTime(oldUser.getExpireTime());
            fresh.setLoginIp(oldUser.getLoginIp());
            fresh.setLoginLocation(oldUser.getLoginLocation());
            fresh.setBrowser(oldUser.getBrowser());
            fresh.setOs(oldUser.getOs());

            long ttl = redisUtil.getExpire(tokenKey);
            if (ttl > 0) {
                redisUtil.set(tokenKey, fresh, Duration.ofSeconds(ttl));
                refreshed++;
            }
        }
        log.info("刷新用户缓存 | userId: {} | tokenCount: {}", userId, refreshed);
    }

    public int getRefreshTokenExpireSeconds() {
        return tokenProperties.getRefreshTokenExpire() * 60;
    }

    public void saveRefreshRememberState(String refreshTokenId, boolean rememberMe) {
        if (StrUtil.isBlank(refreshTokenId)) {
            return;
        }

        redisUtil.set(
                buildRefreshRememberKey(refreshTokenId),
                rememberMe,
                Duration.ofMinutes(tokenProperties.getRefreshTokenExpire())
        );
    }

    public boolean loadRefreshRememberState(String refreshTokenId) {
        if (StrUtil.isBlank(refreshTokenId)) {
            return false;
        }

        Boolean rememberMe = redisUtil.get(buildRefreshRememberKey(refreshTokenId));
        return Boolean.TRUE.equals(rememberMe);
    }

    private LoginUser loadLoginUser(String username) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (!(userDetails instanceof LoginUser loginUser)) {
            throw new BizException(ResultCode.UNAUTHORIZED, "用户信息加载失败");
        }
        return loginUser;
    }

    private void revokeTokenById(String tokenId) {
        if (StrUtil.isBlank(tokenId)) {
            return;
        }

        String tokenKey = buildAccessTokenKey(tokenId);
        LoginUser loginUser = redisUtil.get(tokenKey);
        long remainTtl = redisUtil.getExpire(tokenKey);

        redisUtil.delete(tokenKey);
        redisUtil.sRemove(CommonConstants.ONLINE_TOKENS_KEY, tokenKey);

        String refreshMapKey = buildRefreshMapKey(tokenId);
        String refreshTokenId = redisUtil.get(refreshMapKey);
        if (StrUtil.isBlank(refreshTokenId) && loginUser != null) {
            refreshTokenId = loginUser.getRefreshTokenId();
        }
        if (StrUtil.isNotBlank(refreshTokenId)) {
            redisUtil.delete(buildRefreshTokenKey(refreshTokenId));
            redisUtil.delete(buildRefreshRememberKey(refreshTokenId));
        }
        redisUtil.delete(refreshMapKey);

        String userMapKey = buildUserMapKey(tokenId);
        String userIdStr = redisUtil.get(userMapKey);
        Long userId = null;
        if (StrUtil.isNotBlank(userIdStr)) {
            try {
                userId = Long.parseLong(userIdStr);
            } catch (NumberFormatException ignored) {
            }
        }
        if (userId == null && loginUser != null) {
            userId = loginUser.getUserId();
        }
        if (userId != null) {
            redisUtil.sRemove(USER_TOKEN_PREFIX + userId, tokenId);
        }
        redisUtil.delete(userMapKey);

        if (remainTtl > 0) {
            redisUtil.set(CommonConstants.BLACKLIST_PREFIX + tokenId, "1", Duration.ofSeconds(remainTtl));
        }
    }

    private String generateAccessToken(String tokenId, Long userId, String username,
                                       String deviceId, String clientIp, String userAgent) throws JOSEException {
        return generateJwtToken(tokenId, userId, username, deviceId, clientIp, userAgent,
                tokenProperties.getAccessTokenExpire());
    }

    private String generateRefreshToken(String tokenId, Long userId, String username,
                                        String deviceId, String clientIp, String userAgent,
                                        boolean rememberMe) throws JOSEException {
        return generateJwtToken(tokenId, userId, username, deviceId, clientIp, userAgent,
                tokenProperties.getRefreshTokenExpire());
    }

    private String generateJwtToken(String tokenId, Long userId, String username,
                                    String deviceId, String clientIp, String userAgent,
                                    int expireMinutes) throws JOSEException {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + expireMinutes * 60 * 1000L);

        JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                .jwtID(tokenId)
                .subject(userId.toString())
                .issueTime(now)
                .expirationTime(expireAt);

        if (username != null) {
            claimsBuilder.claim("username", username);
        }
        if (deviceId != null) {
            claimsBuilder.claim("deviceId", deviceId);
        }
        if (clientIp != null) {
            claimsBuilder.claim("clientIp", clientIp);
        }
        if (userAgent != null) {
            claimsBuilder.claim("userAgent", userAgent);
        }

        SignedJWT signedJWT = new SignedJWT(header, claimsBuilder.build());
        signedJWT.sign(new MACSigner(tokenProperties.getSecret().getBytes()));
        return signedJWT.serialize();
    }

    private String parseJwtTokenIdFromAccessToken(String token) throws ParseException, JOSEException {
        SignedJWT signedJWT = SignedJWT.parse(token);
        MACVerifier verifier = new MACVerifier(tokenProperties.getSecret().getBytes());
        if (!signedJWT.verify(verifier)) {
            log.debug("JWT 签名验证失败");
            return null;
        }
        return signedJWT.getJWTClaimsSet().getJWTID();
    }

    private boolean isJwtExpired(String token) throws ParseException {
        SignedJWT signedJWT = SignedJWT.parse(token);
        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
        return claims.getExpirationTime() != null && claims.getExpirationTime().before(new Date());
    }

    /**
     * 自动续期 Redis TTL（非 JWT 有效期）。
     * JWT 的 exp 不变，过期后仍需走 refreshToken 路径重建。
     * 此处仅延长 Redis 中 LoginUser 的存活时间，避免活跃用户的会话数据在 JWT 过期前被清除。
     */
    private void renewIfNeeded(String tokenKey, LoginUser loginUser) {
        long ttl = redisUtil.getExpire(tokenKey);
        long maxTtl = tokenProperties.getAccessTokenExpire() * 60L;
        if (ttl > 0 && ttl < maxTtl * tokenProperties.getRenewThreshold()) {
            Duration newExpire = Duration.ofMinutes(tokenProperties.getAccessTokenExpire());
            redisUtil.expire(tokenKey, newExpire);
            loginUser.setExpireTime(LocalDateTime.now().plusMinutes(tokenProperties.getAccessTokenExpire()));
            redisUtil.set(tokenKey, loginUser, newExpire);
        }
    }

    private void kickOtherDevices(Long userId, String currentTokenId) {
        String userKey = USER_TOKEN_PREFIX + userId;
        Set<String> tokens = redisUtil.sMembers(userKey);
        if (tokens == null || tokens.isEmpty()) {
            return;
        }

        for (String tokenId : tokens) {
            if (!tokenId.equals(currentTokenId)) {
                revokeTokenById(tokenId);
            }
        }
    }

    private String buildAccessTokenKey(String tokenId) {
        return ACCESS_TOKEN_PREFIX + tokenId;
    }

    private String buildRefreshTokenKey(String refreshTokenId) {
        return REFRESH_TOKEN_PREFIX + refreshTokenId;
    }

    private String buildRefreshMapKey(String tokenId) {
        return CommonConstants.REDIS_TOKEN_PREFIX + "refresh-map:" + tokenId;
    }

    private String buildRefreshRememberKey(String refreshTokenId) {
        return REFRESH_REMEMBER_PREFIX + refreshTokenId;
    }

    private String buildUserMapKey(String tokenId) {
        return CommonConstants.REDIS_TOKEN_PREFIX + "user-map:" + tokenId;
    }
}
