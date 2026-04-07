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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
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

    private static final String ACCESS_TOKEN_PREFIX = CommonConstants.REDIS_TOKEN_PREFIX + "access:";
    private static final String REFRESH_TOKEN_PREFIX = CommonConstants.REDIS_TOKEN_PREFIX + "refresh:";
    private static final String USER_TOKEN_PREFIX = CommonConstants.USER_TOKEN_PREFIX;

    private final RedisUtil redisUtil;
    private final TokenProperties tokenProperties;
    private final UserDetailsService userDetailsService;

    /**
     * 创建 Token
     */
    public TokenInfo createToken(LoginUser loginUser, String deviceId, String clientIp, String userAgent) throws JOSEException {
        String normalizedDeviceId = StrUtil.blankToDefault(deviceId, "default");
        String tokenId = IdUtil.fastSimpleUUID();
        String refreshTokenId = IdUtil.fastSimpleUUID();

        String accessToken = generateAccessToken(tokenId, loginUser.getUserId(), loginUser.getUsername(),
                normalizedDeviceId, clientIp, userAgent);
        String refreshToken = generateRefreshToken(refreshTokenId, loginUser.getUserId(), loginUser.getUsername(),
                normalizedDeviceId, clientIp, userAgent);

        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(tokenProperties.getAccessTokenExpire());

        loginUser.setTokenId(tokenId);
        loginUser.setRefreshTokenId(refreshTokenId);
        loginUser.setLoginTime(LocalDateTime.now());
        loginUser.setExpireTime(expireTime);

        long accessExpireMs = tokenProperties.getAccessTokenExpire() * 60 * 1000L;
        long refreshExpireMs = tokenProperties.getRefreshTokenExpire() * 60 * 1000L;

        String accessTokenKey = buildAccessTokenKey(tokenId);
        redisUtil.set(accessTokenKey, loginUser, Duration.ofMillis(accessExpireMs));
        redisUtil.set(buildRefreshTokenKey(refreshTokenId), System.currentTimeMillis(), Duration.ofMillis(refreshExpireMs));

        if (tokenProperties.isSingleDevice()) {
            kickOtherDevices(loginUser.getUserId(), tokenId);
        }

        String userKey = USER_TOKEN_PREFIX + loginUser.getUserId();
        redisUtil.sAdd(userKey, tokenId);
        redisUtil.expire(userKey, Duration.ofMillis(Math.max(accessExpireMs, refreshExpireMs)));
        redisUtil.sAdd(CommonConstants.ONLINE_TOKENS_KEY, accessTokenKey);

        return new TokenInfo()
                .setAccessToken(accessToken)
                .setRefreshToken(refreshToken)
                .setUserId(loginUser.getUserId())
                .setUsername(loginUser.getUsername())
                .setTokenId(tokenId)
                .setDeviceId(normalizedDeviceId)
                .setClientIp(clientIp)
                .setUserAgent(userAgent)
                .setCreatedAt(System.currentTimeMillis())
                .setRefreshCount(0)
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

        // 一次性验证签名并提取所有 claims，避免多次解析未验证 JWT
        SignedJWT signedJWT = SignedJWT.parse(refreshToken);
        MACVerifier verifier = new MACVerifier(tokenProperties.getSecret().getBytes());
        if (!signedJWT.verify(verifier)) {
            log.warn("RefreshToken JWT 签名验证失败");
            return null;
        }

        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
        if (claims.getExpirationTime() != null && claims.getExpirationTime().before(new Date())) {
            log.debug("RefreshToken 已过期");
            return null;
        }

        String refreshTokenId = claims.getJWTID();
        if (refreshTokenId == null) {
            return null;
        }

        if (redisUtil.get(buildRefreshTokenKey(refreshTokenId)) == null) {
            return null;
        }

        String normalizedDeviceId = StrUtil.blankToDefault(deviceId, "default");
        if (tokenProperties.isVerifyClientInfo()) {
            String originalDeviceId = claims.getStringClaim("deviceId");
            String originalClientIp = claims.getStringClaim("clientIp");
            String originalUserAgent = claims.getStringClaim("userAgent");

            if (!StrUtil.equals(originalDeviceId, normalizedDeviceId)
                    || !StrUtil.equals(originalClientIp, clientIp)
                    || !StrUtil.equals(originalUserAgent, userAgent)) {
                log.warn("客户端信息验证失败 | deviceId: {} vs {} | ip: {} vs {}",
                        originalDeviceId, normalizedDeviceId, originalClientIp, clientIp);
                throw new BizException(ResultCode.UNAUTHORIZED, "客户端环境变更，请重新登录");
            }
        }

        redisUtil.delete(buildRefreshTokenKey(refreshTokenId));

        String username = claims.getStringClaim("username");
        LoginUser loginUser = loadLoginUser(username);
        return createToken(loginUser, normalizedDeviceId, clientIp, userAgent);
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

        if (loginUser == null) {
            return;
        }

        if (StrUtil.isNotBlank(loginUser.getRefreshTokenId())) {
            redisUtil.delete(buildRefreshTokenKey(loginUser.getRefreshTokenId()));
        }

        redisUtil.sRemove(USER_TOKEN_PREFIX + loginUser.getUserId(), tokenId);

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
                                        String deviceId, String clientIp, String userAgent) throws JOSEException {
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

    private String parseJwtTokenId(String token) throws ParseException, JOSEException {
        SignedJWT signedJWT = SignedJWT.parse(token);
        MACVerifier verifier = new MACVerifier(tokenProperties.getSecret().getBytes());
        if (!signedJWT.verify(verifier)) {
            log.debug("JWT 签名验证失败");
            return null;
        }

        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
        if (claims.getExpirationTime() != null && claims.getExpirationTime().before(new Date())) {
            log.debug("JWT 已过期");
            return null;
        }
        return claims.getJWTID();
    }

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
        if (tokens == null || tokens.isEmpty()) {
            return;
        }

        for (String tokenId : tokens) {
            if (!tokenId.equals(currentTokenId)) {
                revokeTokenById(tokenId);
            }
        }
        redisUtil.delete(userKey);
    }

    private String buildAccessTokenKey(String tokenId) {
        return ACCESS_TOKEN_PREFIX + tokenId;
    }

    private String buildRefreshTokenKey(String refreshTokenId) {
        return REFRESH_TOKEN_PREFIX + refreshTokenId;
    }
}
