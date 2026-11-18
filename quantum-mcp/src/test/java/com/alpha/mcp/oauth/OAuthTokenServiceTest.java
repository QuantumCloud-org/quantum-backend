package com.alpha.mcp.oauth;

import com.alpha.system.service.impl.UserDetailsServiceImpl;
import com.alpha.cache.util.LocalCacheClient;
import com.alpha.framework.entity.LoginUser;
import com.alpha.framework.exception.BizException;
import com.alpha.mcp.config.McpProperties;
import com.alpha.security.token.TokenProperties;
import com.alpha.security.token.TokenService;
import com.alpha.security.token.SessionGeneration;
import com.alpha.system.security.UserSessionInvalidationService;
import com.alpha.system.service.impl.SysOnlineServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.transaction.event.TransactionalEventListenerFactory;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuthTokenServiceTest {

    private McpProperties properties;
    private OAuthTokenService tokenService;
    private UserDetailsServiceImpl userDetails;
    private LocalCacheClient cache;
    private SessionGeneration generations;

    @BeforeEach
    void setUp() {
        properties = new McpProperties();
        properties.setEnabled(true);
        var client = new McpProperties.Client();
        client.setRedirectUris(java.util.List.of("http://127.0.0.1/callback"));
        client.setScopes(properties.getDefaultScopes());
        properties.getClients().put("quantum-local-agent", client);
        userDetails = org.mockito.Mockito.mock(UserDetailsServiceImpl.class);
        org.mockito.Mockito.when(userDetails.loadUserById(2L)).thenAnswer(invocation -> loginUser());
        cache = new LocalCacheClient();
        generations = new SessionGeneration(cache);
        tokenService = new OAuthTokenService(cache, properties, userDetails, generations);
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
                .extracting(token -> token.user().getUsername())
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

    @Test
    void temporaryUserLookupFailureLeavesRefreshUsableWithoutRevokingOtherGrants() {
        String client = "quantum-local-agent";
        String redirect = "http://127.0.0.1/callback";
        String verifier = "test-verifier";
        String resource = properties.getResource();
        var first = tokenService.exchangeAuthorizationCode(client, redirect,
                tokenService.createAuthorizationCode(authRequest(verifier), loginUser()), verifier, resource);
        var other = tokenService.exchangeAuthorizationCode(client, redirect,
                tokenService.createAuthorizationCode(authRequest(verifier), loginUser()), verifier, resource);

        var failure = new IllegalStateException("temporary user store failure");
        org.mockito.Mockito.when(userDetails.loadUserById(2L)).thenThrow(failure).thenAnswer(invocation -> loginUser());
        assertThatThrownBy(() -> tokenService.refresh(first.getRefreshToken(), client, resource)).isSameAs(failure);
        var rotated = tokenService.refresh(first.getRefreshToken(), client, resource);
        assertThat(tokenService.validateAccessToken(rotated.getAccessToken(), resource)).isNotNull();
        assertThat(tokenService.validateAccessToken(other.getAccessToken(), resource)).isNotNull();
    }

    @Test
    void missingVerifierIsRejectedAsABusinessError() {
        String verifier = "valid-test-verifier";
        String code = tokenService.createAuthorizationCode(authRequest(verifier), loginUser());
        assertThatThrownBy(() -> tokenService.exchangeAuthorizationCode(
                "quantum-local-agent", "http://127.0.0.1/callback", code, null, properties.getResource()))
                .isInstanceOf(BizException.class);
    }

    @Test
    void readsCurrentPermissionsAndRejectsDisabledSessions() {
        String verifier = "test-verifier";
        String code = tokenService.createAuthorizationCode(authRequest(verifier), loginUser());
        var tokens = tokenService.exchangeAuthorizationCode(
                "quantum-local-agent", "http://127.0.0.1/callback", code, verifier, properties.getResource());

        org.mockito.Mockito.when(userDetails.loadUserById(2L)).thenReturn(loginUser().setPermissions(Set.of("system:new:list")));
        assertThat(tokenService.validateAccessToken(tokens.getAccessToken(), properties.getResource()).user().getPermissions())
                .containsExactly("system:new:list");
        org.mockito.Mockito.when(userDetails.loadUserById(2L)).thenReturn(loginUser().setStatus(0));
        assertThat(tokenService.validateAccessToken(tokens.getAccessToken(), properties.getResource())).isNull();
        assertThatThrownBy(() -> tokenService.refresh(tokens.getRefreshToken(), "quantum-local-agent", properties.getResource()))
                .isInstanceOf(BizException.class);
    }

    @Test
    void forceLogoutInvalidatesExistingAccessRefreshAndAuthorizationCodes() {
        String verifier = "test-verifier";
        String code = tokenService.createAuthorizationCode(authRequest(verifier), loginUser());
        var tokens = tokenService.exchangeAuthorizationCode(
                "quantum-local-agent", "http://127.0.0.1/callback", code, verifier, properties.getResource());
        String pendingCode = tokenService.createAuthorizationCode(authRequest(verifier), loginUser());

        generations.revoke(2L);

        assertThat(tokenService.validateAccessToken(tokens.getAccessToken(), properties.getResource())).isNull();
        assertThatThrownBy(() -> tokenService.refresh(tokens.getRefreshToken(), "quantum-local-agent", properties.getResource()))
                .isInstanceOf(BizException.class);
        assertThatThrownBy(() -> tokenService.exchangeAuthorizationCode(
                "quantum-local-agent", "http://127.0.0.1/callback", pendingCode, verifier, properties.getResource()))
                .isInstanceOf(BizException.class);
    }

    @Test
    void monitorForceLogoutRevokesWebAndMcpSessionsButSingleSessionActionIsWebOnly() throws Exception {
        String verifier = "test-verifier";
        String code = tokenService.createAuthorizationCode(authRequest(verifier), loginUser());
        var oauth = tokenService.exchangeAuthorizationCode(
                "quantum-local-agent", "http://127.0.0.1/callback", code, verifier, properties.getResource());
        var webProperties = new TokenProperties();
        webProperties.setSecret("test-fake-signing-key-".repeat(4));
        var webTokens = new TokenService(cache, webProperties, username -> loginUser(), generations);
        var first = webTokens.createToken(loginUser(), generations.open(2L), null, null, null, false);
        var second = webTokens.createToken(loginUser(), generations.open(2L), null, null, null, false);

        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean(TokenService.class, () -> webTokens);
            context.registerBean(OAuthTokenService.class, () -> tokenService);
            context.registerBean(TransactionalEventListenerFactory.class);
            context.registerBean(UserSessionInvalidationService.class);
            context.registerBean(SysOnlineServiceImpl.class);
            context.refresh();
            var online = context.getBean(SysOnlineServiceImpl.class);
            online.forceLogout(first.getTokenId());
            assertThat(webTokens.validateToken(first.getAccessToken())).isNull();
            assertThat(webTokens.validateToken(second.getAccessToken())).isNotNull();
            assertThat(tokenService.validateAccessToken(oauth.getAccessToken(), properties.getResource())).isNotNull();

            online.forceLogoutByUserId(2L);

            assertThat(webTokens.validateToken(second.getAccessToken())).isNull();
            assertThat(tokenService.validateAccessToken(oauth.getAccessToken(), properties.getResource())).isNull();
            assertThatThrownBy(() -> tokenService.refresh(oauth.getRefreshToken(), "quantum-local-agent", properties.getResource()))
                    .isInstanceOf(BizException.class);
        }
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

    private LoginUser loginUser() {
        return new LoginUser()
                .setUserId(2L)
                .setUsername("alice")
                .setNickname("Alice")
                .setStatus(1)
                .setSessionGeneration(generations.open(2L))
                .setPermissions(Set.of("system:user:list"))
                .setRoles(Set.of("user"));
    }
}
