package com.alpha.security.token;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alpha.cache.util.RedisUtil;
import com.alpha.framework.constant.CommonConstants;
import com.alpha.framework.entity.LoginUser;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Set;

/**
 * Token 服务 - JWT + Redis 混合方案
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final RedisUtil redisUtil;
    private final TokenProperties tokenProperties;

    // Redis Key 前缀
    private static final String ACCESS_TOKEN_PREFIX = CommonConstants.REDIS_TOKEN_PREFIX + "access:";
    private static final String REFRESH_TOKEN_PREFIX = CommonConstants.REDIS_TOKEN_PREFIX + "refresh:";
    private static final String USER_TOKEN_PREFIX = CommonConstants.USER_TOKEN_PREFIX;

    /**
     * 创建 Token
     */
    public TokenInfo createToken(LoginUser loginUser, String deviceId, String clientIp, String userAgent) throws JOSEException {
        String tokenId = IdUtil.fastSimpleUUID();
        String refreshTokenId = IdUtil.fastSimpleUUID();

        // accessToken 包含完整信息
        String accessToken = generateJwtToken(tokenId, loginUser.getUserId(), loginUser.getUsername(), deviceId, clientIp, userAgent);
        // refreshToken 只有 username
        String refreshToken = generateJwtToken(refreshTokenId, loginUser.getUserId(), loginUser.getUsername(), null, null, null);

        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(tokenProperties.getAccessTokenExpire());

        loginUser.setToken(accessToken);
        loginUser.setRefreshToken(refreshToken);
        loginUser.setLoginTime(LocalDateTime.now());
        loginUser.setExpireTime(expireTime);

        // 存储到 Redis
        long accessExpireMs = tokenProperties.getAccessTokenExpire() * 60 * 1000L;
        long refreshExpireMs = tokenProperties.getRefreshTokenExpire() * 60 * 1000L;

        redisUtil.set(ACCESS_TOKEN_PREFIX + tokenId, loginUser, Duration.ofMillis(accessExpireMs));
        redisUtil.set(REFRESH_TOKEN_PREFIX + refreshTokenId, System.currentTimeMillis(), Duration.ofMillis(refreshExpireMs));

        // 单设备登录：踢掉其他设备
        if (tokenProperties.isSingleDevice()) {
            kickOtherDevices(loginUser.getUserId(), tokenId);
        }

        // 记录用户 Token
        String userKey = USER_TOKEN_PREFIX + loginUser.getUserId();
        redisUtil.sAdd(userKey, tokenId);
        redisUtil.expire(userKey, Duration.ofMillis(accessExpireMs));

        return new TokenInfo()
                .setAccessToken(accessToken)
                .setRefreshToken(refreshToken)
                .setExpireTime(expireTime);
    }

    /**
     * 验证 Token 并返回用户信息
     * 支持 JWT 过期但 Redis 有数据时自动续期 TTL
     */
    public LoginUser validateToken(String token) throws ParseException, JOSEException {
        if (StrUtil.isBlank(token)) {
            return null;
        }

        // 解析 JWT（只验证签名，不过期检查）
        String tokenId = parseJwtTokenIdFromAccessToken(token);
        if (tokenId == null) {
            log.warn("【TokenService】JWT 签名验证失败或 TokenID 为空");
            return null;
        }

        String tokenKey = ACCESS_TOKEN_PREFIX + tokenId;
        // 检查黑名单
        if (redisUtil.exists(CommonConstants.BLACKLIST_PREFIX + tokenId)) {
            log.warn("【TokenService】Token 在黑名单中 | TokenId: {}", tokenId);
            return null;
        }

        LoginUser loginUser = redisUtil.get(tokenKey);
        if (loginUser == null) {
            log.warn("【TokenService】Redis 中找不到 Token | TokenKey: {}", tokenKey);
            return null;
        }

        // 检查 JWT 是否过期，如果过期则强制续期
        boolean jwtExpired = isJwtExpired(token);
        if (jwtExpired) {
            forceRenew(tokenKey, loginUser);
        } else {
            // 自动续期（基于阈值）
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

        String refreshTokenId = parseJwtTokenId(refreshToken);
        if (refreshTokenId == null) {
            return null;
        }

        Long createdAt = redisUtil.get(REFRESH_TOKEN_PREFIX + refreshTokenId);
        if (createdAt == null) {
            return null;
        }

        // 验证客户端信息
        if (tokenProperties.isVerifyClientInfo()) {
            String originalDeviceId = parseJwtDeviceId(refreshToken);
            String originalClientIp = parseJwtClientIp(refreshToken);
            String originalUserAgent = parseJwtUserAgent(refreshToken);

            if (!StrUtil.equals(originalDeviceId, deviceId) ||
                !StrUtil.equals(originalClientIp, clientIp) ||
                !StrUtil.equals(originalUserAgent, userAgent)) {
                log.warn("客户端信息验证失败");
            }
        }

        // 删除旧 RefreshToken
        redisUtil.delete(REFRESH_TOKEN_PREFIX + refreshTokenId);

        // 从 JWT 获取用户信息
        String username = parseJwtUsername(refreshToken);
        Long userId = Long.parseLong(parseJwtSubject(refreshToken));

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(userId);
        loginUser.setUsername(username);

        return createToken(loginUser, deviceId, clientIp, userAgent);
    }

    /**
     * 登出
     */
    public void logout(String token) throws ParseException, JOSEException {
        if (StrUtil.isBlank(token)) {
            return;
        }

        // 解析 JWT（只验证签名，不过期检查）
        String tokenId = parseJwtTokenIdFromAccessToken(token);
        if (tokenId == null) {
            return;
        }

        String tokenKey = ACCESS_TOKEN_PREFIX + tokenId;
        LoginUser loginUser = redisUtil.get(tokenKey);

        if (loginUser != null) {
            long remainTtl = redisUtil.getExpire(tokenKey);

            redisUtil.delete(tokenKey);

            // 删除 RefreshToken
            if (StrUtil.isNotBlank(loginUser.getRefreshToken())) {
                String refreshTokenId = parseJwtTokenId(loginUser.getRefreshToken());
                if (refreshTokenId != null) {
                    redisUtil.delete(REFRESH_TOKEN_PREFIX + refreshTokenId);
                }
            }

            // 移除用户 Token 记录
            redisUtil.sRemove(USER_TOKEN_PREFIX + loginUser.getUserId(), tokenId);

            // 加入黑名单
            if (remainTtl > 0) {
                redisUtil.set(CommonConstants.BLACKLIST_PREFIX + tokenId, "1", Duration.ofSeconds(remainTtl));
            }

            log.info("用户登出 | UserId: {}", loginUser.getUserId());
        }
    }

    /**
     * 强制下线用户
     */
    public void kickOut(Long userId) {
        String userKey = USER_TOKEN_PREFIX + userId;
        Set<String> tokens = redisUtil.sMembers(userKey);

        if (tokens != null && !tokens.isEmpty()) {
            for (String tokenId : tokens) {
                String tokenKey = ACCESS_TOKEN_PREFIX + tokenId;
                long remainTtl = redisUtil.getExpire(tokenKey);
                redisUtil.delete(tokenKey);

                if (remainTtl > 0) {
                    redisUtil.set(CommonConstants.BLACKLIST_PREFIX + tokenId, "1", Duration.ofSeconds(remainTtl));
                }
            }
            redisUtil.delete(userKey);
            log.info("强制下线用户 | UserId: {} | Count: {}", userId, tokens.size());
        }
    }

    // ==================== JWT 方法 ====================

    /**
     * 生成 JWT Token (HS512)
     * @param tokenId 令牌 ID
     * @param userId 用户 ID
     * @param username 用户名
     * @param deviceId 设备标识（可为 null）
     * @param clientIp 客户端 IP（可为 null）
     * @param userAgent User-Agent（可为 null）
     */
    private String generateJwtToken(String tokenId, Long userId, String username, String deviceId, String clientIp, String userAgent) throws JOSEException {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + tokenProperties.getAccessTokenExpire() * 60 * 1000L);

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

    /**
     * 解析 AccessToken（只验证签名，不过期检查）
     */
    private String parseJwtTokenIdFromAccessToken(String token) throws ParseException, JOSEException {
        SignedJWT signedJWT = SignedJWT.parse(token);

        // 签名验证
        MACVerifier verifier = new MACVerifier(tokenProperties.getSecret().getBytes());
        if (!signedJWT.verify(verifier)) {
            log.debug("JWT 签名验证失败");
            return null;
        }

        return signedJWT.getJWTClaimsSet().getJWTID();
    }

    /**
     * 解析 RefreshToken（包含签名验证和过期检查）
     */
    private String parseJwtTokenId(String token) throws ParseException, JOSEException {
        SignedJWT signedJWT = SignedJWT.parse(token);

        // 签名验证
        MACVerifier verifier = new MACVerifier(tokenProperties.getSecret().getBytes());
        if (!signedJWT.verify(verifier)) {
            log.debug("JWT 签名验证失败");
            return null;
        }

        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

        // 检查过期
        if (claims.getExpirationTime() != null && claims.getExpirationTime().before(new Date())) {
            log.debug("JWT 已过期");
            return null;
        }

        return claims.getJWTID();
    }

    private String parseJwtSubject(String token) throws ParseException {
        SignedJWT signedJWT = SignedJWT.parse(token);
        return signedJWT.getJWTClaimsSet().getSubject();
    }

    private String parseJwtUsername(String token) throws ParseException {
        SignedJWT signedJWT = SignedJWT.parse(token);
        return signedJWT.getJWTClaimsSet().getStringClaim("username");
    }

    private String parseJwtDeviceId(String token) throws ParseException {
        SignedJWT signedJWT = SignedJWT.parse(token);
        return signedJWT.getJWTClaimsSet().getStringClaim("deviceId");
    }

    private String parseJwtClientIp(String token) throws ParseException {
        SignedJWT signedJWT = SignedJWT.parse(token);
        return signedJWT.getJWTClaimsSet().getStringClaim("clientIp");
    }

    private String parseJwtUserAgent(String token) throws ParseException {
        SignedJWT signedJWT = SignedJWT.parse(token);
        return signedJWT.getJWTClaimsSet().getStringClaim("userAgent");
    }

    // ==================== 内部方法 ====================

    /**
     * 强制续期（用于 JWT 已过期但 Redis 有数据的情况）
     */
    private void forceRenew(String tokenKey, LoginUser loginUser) {
        Duration newExpire = Duration.ofMinutes(tokenProperties.getAccessTokenExpire());
        redisUtil.expire(tokenKey, newExpire);

        loginUser.setExpireTime(LocalDateTime.now().plusMinutes(tokenProperties.getAccessTokenExpire()));
        redisUtil.set(tokenKey, loginUser, newExpire);
    }

    /**
     * 检查 JWT 是否过期
     */
    private boolean isJwtExpired(String token) throws ParseException {
        SignedJWT signedJWT = SignedJWT.parse(token);
        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
        return claims.getExpirationTime() != null && claims.getExpirationTime().before(new Date());
    }

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

        if (tokens != null) {
            for (String tokenId : tokens) {
                if (!tokenId.equals(currentTokenId)) {
                    String tokenKey = ACCESS_TOKEN_PREFIX + tokenId;
                    long remainTtl = redisUtil.getExpire(tokenKey);
                    redisUtil.delete(tokenKey);

                    if (remainTtl > 0) {
                        redisUtil.set(CommonConstants.BLACKLIST_PREFIX + tokenId, "1", Duration.ofSeconds(remainTtl));
                    }
                }
            }
            redisUtil.delete(userKey);
        }
    }

}