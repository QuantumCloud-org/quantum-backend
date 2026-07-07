package com.alpha.mcp.oauth;

import com.alpha.cache.util.LocalCacheClient;
import com.alpha.framework.entity.LoginUser;
import com.alpha.framework.exception.BizException;
import com.alpha.mcp.config.McpProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuthTokenServiceTest {

    private McpProperties properties;
    private OAuthTokenService tokenService;

    @BeforeEach
    void setUp() {
        properties = new McpProperties();
        properties.setEnabled(true);
        tokenService = new OAuthTokenService(new LocalCacheClient(), properties);
    }

    @Test
    void authorizationCodeIsOneTimeAndBindsPkceClientRedirectAndResource() {
        String verifier = "test-verifier-with-enough-length-123";
        String code = tokenService.createAuthorizationCode(authRequest(verifier), loginUser());

        OAuthTokenResponse response = tokenService.exchangeAuthorizationCode(
                "quantum-local-agent",
                "http://127.0.0.1/callback",
                code,
                verifier,
                properties.getResource());

        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(tokenService.validateAccessToken(response.getAccessToken(), properties.getResource()))
                .extracting(token -> token.getLoginUser().getUsername())
                .isEqualTo("alice");
        assertThatThrownBy(() -> tokenService.exchangeAuthorizationCode(
                "quantum-local-agent",
                "http://127.0.0.1/callback",
                code,
                verifier,
                properties.getResource()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("already used");
    }

    @Test
    void pkceFailureRejectsCodeExchange() {
        String code = tokenService.createAuthorizationCode(authRequest("good-verifier-123"), loginUser());

        assertThatThrownBy(() -> tokenService.exchangeAuthorizationCode(
                "quantum-local-agent",
                "http://127.0.0.1/callback",
                code,
                "bad-verifier-123",
                properties.getResource()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("PKCE");
    }

    @Test
    void resourceMismatchDoesNotValidateAccessToken() {
        String verifier = "test-verifier-with-enough-length-456";
        String code = tokenService.createAuthorizationCode(authRequest(verifier), loginUser());
        OAuthTokenResponse response = tokenService.exchangeAuthorizationCode(
                "quantum-local-agent",
                "http://127.0.0.1/callback",
                code,
                verifier,
                properties.getResource());

        assertThat(tokenService.validateAccessToken(response.getAccessToken(), "http://localhost:8080/other"))
                .isNull();
    }

    @Test
    void refreshTokenRotatesAndReuseRevokesClientTokenChain() {
        String verifier = "test-verifier-with-enough-length-789";
        String code = tokenService.createAuthorizationCode(authRequest(verifier), loginUser());
        OAuthTokenResponse first = tokenService.exchangeAuthorizationCode(
                "quantum-local-agent",
                "http://127.0.0.1/callback",
                code,
                verifier,
                properties.getResource());

        OAuthTokenResponse second = tokenService.refresh(
                first.getRefreshToken(),
                "quantum-local-agent",
                properties.getResource());

        assertThat(tokenService.validateAccessToken(first.getAccessToken(), properties.getResource())).isNull();
        assertThat(tokenService.validateAccessToken(second.getAccessToken(), properties.getResource())).isNotNull();

        assertThatThrownBy(() -> tokenService.refresh(
                first.getRefreshToken(),
                "quantum-local-agent",
                properties.getResource()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("already used");
        assertThat(tokenService.validateAccessToken(second.getAccessToken(), properties.getResource())).isNull();
    }

    private OAuthTokenService.AuthorizationRequest authRequest(String verifier) {
        return new OAuthTokenService.AuthorizationRequest(
                "quantum-local-agent",
                "http://127.0.0.1/callback",
                PkceUtil.s256Challenge(verifier),
                PkceUtil.S256,
                properties.getResource(),
                Set.of("system.user.read"),
                "nonce-1"
        );
    }

    private static LoginUser loginUser() {
        return new LoginUser()
                .setUserId(2L)
                .setUsername("alice")
                .setNickname("Alice")
                .setStatus(1)
                .setPermissions(Set.of("system:user:list"))
                .setRoles(Set.of("user"));
    }
}
