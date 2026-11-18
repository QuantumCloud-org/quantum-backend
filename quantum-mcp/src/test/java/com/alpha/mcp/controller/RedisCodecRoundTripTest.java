package com.alpha.mcp.controller;

import com.alpha.cache.config.RedisConfig;
import com.alpha.cache.config.RedissonPoolProperties;
import com.alpha.framework.entity.LoginUser;
import com.alpha.mcp.oauth.OAuthAccessToken;
import com.alpha.mcp.oauth.OAuthAuthorizationCode;
import com.alpha.mcp.oauth.OAuthRefreshToken;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import org.redisson.client.handler.State;
import org.redisson.config.Config;
import org.springframework.util.LinkedCaseInsensitiveMap;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RedisCodecRoundTripTest {
    private final Config config = new Config();

    RedisCodecRoundTripTest() {
        new RedisConfig(new RedissonPoolProperties()).redissonCustomizer().customize(config);
    }

    @Test
    void preservesWebAndMcpCacheEntries() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 11, 18, 11, 15);
        Set<String> scopes = new HashSet<>(Set.of("system.user.read"));
        LoginUser user = new LoginUser().setUsername("alice").setUserId(2L).setLoginTime(now)
                .setPermissions(new HashSet<>(Set.of("system:user:list")));
        var access = new OAuthAccessToken().setUserId(2L).setIssuedAt(now).setScopes(scopes);
        var refresh = new OAuthRefreshToken().setUserId(2L).setIssuedAt(now).setScopes(scopes);
        var code = new OAuthAuthorizationCode().setUserId(2L).setIssuedAt(now).setScopes(scopes);

        assertThat(roundTrip(user)).isEqualTo(user);
        assertThat(roundTrip(access)).isEqualTo(access);
        assertThat(roundTrip(refresh)).isEqualTo(refresh);
        assertThat(roundTrip(code)).isEqualTo(code);
    }

    @Test
    void rejectsNetworkTypeIds() {
        assertThatThrownBy(() -> decodeTypeId("""
                ["java.net.URL","https://example.invalid"]
                """)).isInstanceOf(InvalidTypeIdException.class);
    }

    @Test
    void rejectsNetworkArrayTypeIds() {
        assertThatThrownBy(() -> decodeTypeId("""
                ["[Ljava.net.URL;",["https://example.invalid"]]
                """)).isInstanceOf(InvalidTypeIdException.class);
    }

    @Test
    void acceptsPrimitiveArrayTypeIds() throws Exception {
        assertThat(decodeTypeId("""
                ["[[I",[[1,2],[3]]]
                """)).isEqualTo(new int[][]{{1, 2}, {3}});
    }

    private Object decodeTypeId(String json) throws Exception {
        var data = Unpooled.copiedBuffer(json, StandardCharsets.UTF_8);
        try {
            return config.getCodec().getValueDecoder().decode(data, new State());
        } finally {
            data.release();
        }
    }

    @Test
    void rejectsATypeOutsideTheCacheAllowlist() {
        var unrelatedMap = new LinkedCaseInsensitiveMap<String>();
        unrelatedMap.put("key", "sample");
        assertThatThrownBy(() -> roundTrip(unrelatedMap)).isInstanceOf(java.io.IOException.class);
    }

    private Object roundTrip(Object value) throws Exception {
        var codec = config.getCodec();
        var data = codec.getValueEncoder().encode(value);
        try {
            return codec.getValueDecoder().decode(data, new State());
        } finally {
            data.release();
        }
    }
}
