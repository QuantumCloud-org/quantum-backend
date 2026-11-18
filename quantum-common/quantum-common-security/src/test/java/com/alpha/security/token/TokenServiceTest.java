package com.alpha.security.token;

import com.alpha.cache.util.CacheClient;
import com.alpha.cache.util.LocalCacheClient;
import com.alpha.framework.constant.CommonConstants;
import com.alpha.framework.entity.LoginUser;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.time.Instant;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TokenServiceTest {

    private static final String TOKEN_INPUT = "synthetic-input";
    private static final String TOKEN_ID = "synthetic-session";

    private final CacheClient cache = mock(CacheClient.class);
    private final UserDetailsService users = mock(UserDetailsService.class);
    private final TokenProperties properties = testProperties();
    private final TokenService service = new TokenService(cache, properties, users, new SessionGeneration(cache));

    @Test
    void accessValidationRequiresAnExpiration() throws Exception {
        var claims = new JWTClaimsSet.Builder().jwtID(TOKEN_ID).build();

        assertRejectedForAccess(JWSAlgorithm.HS512, claims);
    }

    @Test
    void accessValidationRequiresTheServiceSigningAlgorithm() throws Exception {
        var claims = new JWTClaimsSet.Builder()
                .jwtID(TOKEN_ID)
                .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                .build();

        assertRejectedForAccess(JWSAlgorithm.HS256, claims);
    }

    @Test
    void refreshValidationRequiresAnExpirationBeforeLookingUpTheSession() throws Exception {
        var claims = new JWTClaimsSet.Builder().jwtID(TOKEN_ID).claim("username", "synthetic-user").build();
        var jwt = parsedJwt(JWSAlgorithm.HS512, claims);

        try (var parser = mockStatic(SignedJWT.class)) {
            parser.when(() -> SignedJWT.parse(TOKEN_INPUT)).thenReturn(jwt);

            assertThat(service.refreshToken(TOKEN_INPUT, null, null, null)).isNull();
        }
        verifyNoInteractions(cache, users);
    }

    @Test
    void accessValidationDoesNotRewriteTheSession() throws Exception {
        var claims = new JWTClaimsSet.Builder().jwtID(TOKEN_ID)
                .claim("sessionId", "web-test").claim("kind", "access")
                .claim("generation", "test-generation").subject("2")
                .expirationTime(Date.from(Instant.now().plusSeconds(30))).build();
        var jwt = parsedJwt(JWSAlgorithm.HS512, claims);
        String key = CommonConstants.REDIS_TOKEN_PREFIX + "access:" + TOKEN_ID;
        String sessionKey = CommonConstants.REDIS_TOKEN_PREFIX + "web-session:web-test";
        String generationKey = SessionGeneration.key(2L);
        var keys = List.of(key, sessionKey, generationKey);
        when(cache.getAll(keys)).thenReturn(Map.of(key,
                activeUser().setWebSessionId("web-test").setSessionGeneration("test-generation"),
                sessionKey, "test-refresh-id", generationKey, "test-generation"));

        try (var parser = mockStatic(SignedJWT.class)) {
            parser.when(() -> SignedJWT.parse(TOKEN_INPUT)).thenReturn(jwt);
            assertThat(service.validateToken(TOKEN_INPUT)).isNotNull();
        }
        org.mockito.Mockito.verify(cache).getAll(keys);
        org.mockito.Mockito.verifyNoMoreInteractions(cache);
    }

    @Test
    void loadsPermissionsOnceForMultipleSessions() {
        String prefix = CommonConstants.REDIS_TOKEN_PREFIX + "access:";
        when(cache.<String>zRangeByScore(org.mockito.ArgumentMatchers.eq(CommonConstants.USER_TOKEN_PREFIX + "live:" + 2L),
                org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.eq(Double.POSITIVE_INFINITY)))
                .thenReturn(List.of("one", "two"));
        when(cache.<LoginUser>getAll(org.mockito.ArgumentMatchers.anyCollection())).thenReturn(java.util.Map.of(
                prefix + "one", new LoginUser().setUserId(2L).setUsername("alice").setTokenId("one"),
                prefix + "two", new LoginUser().setUserId(2L).setUsername("alice").setTokenId("two")));
        when(users.loadUserByUsername("alice")).thenReturn(new LoginUser().setUserId(2L).setUsername("alice").setStatus(1));

        service.refreshUserCache(2L);

        org.mockito.Mockito.verify(users).loadUserByUsername("alice");
    }

    @Test
    void forceLogoutDuringRegistrationRejectsAnyRefreshFromTheOldGeneration() throws Exception {
        var sharedCache = new RegistrationCache();
        var generations = new SessionGeneration(sharedCache);
        var firstNode = new TokenService(sharedCache, properties, username -> activeUser(), generations);
        var secondNode = new TokenService(sharedCache, properties, username -> activeUser(), new SessionGeneration(sharedCache));
        var old = firstNode.createToken(activeUser(), generations.open(2L), null, null, null, false);
        sharedCache.onAccessRegistration = () -> secondNode.kickOut(2L);

        assertThat(firstNode.refreshToken(old.getRefreshToken(), null, null, null)).isNull();
        assertThat(firstNode.validateToken(old.getAccessToken())).isNull();
        assertThat(firstNode.refreshToken(old.getRefreshToken(), null, null, null)).isNull();
    }

    @Test
    void revokedEvenWhenUserIndexWasEvicted() throws Exception {
        var local = new LocalCacheClient();
        var generations = new SessionGeneration(local);
        var tokens = new TokenService(local, properties, username -> activeUser(), generations);
        var old = tokens.createToken(activeUser(), generations.open(2L), null, null, null, false);
        local.delete(CommonConstants.USER_TOKEN_PREFIX + "live:" + 2L);
        assertThat(tokens.validateToken(old.getAccessToken())).isNotNull();

        tokens.kickOut(2L);

        assertThat(tokens.validateToken(old.getAccessToken())).isNull();
        assertThat(tokens.refreshToken(old.getRefreshToken(), null, null, null)).isNull();
    }

    @Test
    void temporaryRegistrationFailureLeavesPreviousRefreshUsable() throws Exception {
        var local = new RegistrationCache();
        var generations = new SessionGeneration(local);
        var tokens = new TokenService(local, properties, username -> activeUser(), generations);
        var old = tokens.createToken(activeUser(), generations.open(2L), null, null, null, false);
        local.onAccessRegistration = () -> { throw new IllegalStateException("temporary cache issue"); };

        assertThatThrownBy(() -> tokens.refreshToken(old.getRefreshToken(), null, null, null))
                .isInstanceOf(IllegalStateException.class);
        assertThat(tokens.refreshToken(old.getRefreshToken(), null, null, null)).isNotNull();
    }

    private static LoginUser activeUser() {
        return new LoginUser().setUserId(2L).setUsername("alice").setStatus(1);
    }

    private static class RegistrationCache extends LocalCacheClient {
        Runnable onAccessRegistration;

        @Override
        public <T> void set(String key, T value, Duration ttl) {
            if (key.startsWith(CommonConstants.REDIS_TOKEN_PREFIX + "access:") && onAccessRegistration != null) {
                var action = onAccessRegistration;
                onAccessRegistration = null;
                action.run();
            }
            super.set(key, value, ttl);
        }
    }

    private void assertRejectedForAccess(JWSAlgorithm algorithm, JWTClaimsSet claims) throws Exception {
        var jwt = parsedJwt(algorithm, claims);
        when(cache.get(CommonConstants.REDIS_TOKEN_PREFIX + "access:" + TOKEN_ID))
                .thenReturn(new LoginUser().setStatus(1));

        try (var parser = mockStatic(SignedJWT.class)) {
            parser.when(() -> SignedJWT.parse(TOKEN_INPUT)).thenReturn(jwt);

            assertThat(service.validateToken(TOKEN_INPUT)).isNull();
        }
    }

    private SignedJWT parsedJwt(JWSAlgorithm algorithm, JWTClaimsSet claims) throws Exception {
        var jwt = mock(SignedJWT.class);
        when(jwt.getHeader()).thenReturn(new JWSHeader(algorithm));
        when(jwt.verify(any(MACVerifier.class))).thenReturn(true);
        when(jwt.getJWTClaimsSet()).thenReturn(claims);
        return jwt;
    }
    private static TokenProperties testProperties() {
        var properties = new TokenProperties();
        properties.setSecret("synthetic".repeat(8));
        return properties;
    }
}
