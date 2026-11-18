package com.alpha.mcp.security;

import com.alpha.cache.util.LocalCacheClient;
import com.alpha.framework.context.UserContext;
import com.alpha.framework.entity.LoginUser;
import com.alpha.mcp.config.McpProperties;
import com.alpha.mcp.oauth.OAuthTokenResponse;
import com.alpha.mcp.oauth.OAuthTokenService;
import com.alpha.mcp.oauth.PkceUtil;
import com.alpha.security.token.SessionGeneration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthBearerAuthenticationFilterTest {

    private McpProperties properties;
    private OAuthTokenService tokenService;
    private OAuthBearerAuthenticationFilter filter;
    private SessionGeneration generations;

    @BeforeEach
    void setUp() {
        properties = new McpProperties();
        properties.setEnabled(true);
        var client = new McpProperties.Client();
        client.setRedirectUris(java.util.List.of("http://127.0.0.1/callback"));
        client.setScopes(properties.getDefaultScopes());
        properties.getClients().put("quantum-local-agent", client);
        var userDetails = org.mockito.Mockito.mock(com.alpha.system.service.impl.UserDetailsServiceImpl.class);
        org.mockito.Mockito.when(userDetails.loadUserById(2L)).thenAnswer(invocation -> loginUser());
        var cache = new LocalCacheClient();
        generations = new SessionGeneration(cache);
        tokenService = new OAuthTokenService(cache, properties, userDetails, generations);
        filter = new OAuthBearerAuthenticationFilter(tokenService, properties, new com.alpha.framework.util.ServletJsonWriter(new ObjectMapper()));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsMissingBearerTokenWithProtectedResourceMetadata() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mcp");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.doFilter(request, response, (req, res) -> chainCalled.set(true));

        assertThat(chainCalled).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader("WWW-Authenticate"))
                .contains("Bearer")
                .contains("/.well-known/oauth-protected-resource");
    }

    @Test
    void acceptsValidMcpOAuthTokenAndSetsUserContext() throws Exception {
        OAuthTokenResponse token = issueToken();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mcp");
        request.addHeader("Authorization", "Bearer " + token.getAccessToken());
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.doFilter(request, response, (req, res) -> {
            chainCalled.set(true);
            assertThat(UserContext.getUser()).isNotNull();
            assertThat(UserContext.getUsername()).isEqualTo("alice");
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication().getDetails())
                    .isInstanceOfSatisfying(McpAuthenticationDetails.class,
                            details -> assertThat(details.hasScope("system.user.read")).isTrue());
        });

        assertThat(chainCalled).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void rejectsMissingBearerTokenWithServletContextPath() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/quantum/mcp");
        request.setContextPath("/quantum");

        assertMcpAuthenticationRequired(request);
    }

    @Test
    void rejectsMissingBearerTokenAtTheMappedEndpointEvenWhenConfigurationDiffers() throws Exception {
        properties.setEndpoint("/configured-mcp");

        assertMcpAuthenticationRequired(new MockHttpServletRequest("GET", "/mcp"));
    }

    private void assertMcpAuthenticationRequired(MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean();

        filter.doFilter(request, response, (req, res) -> chainCalled.set(true));

        assertThat(chainCalled).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }

    private OAuthTokenResponse issueToken() {
        String verifier = "filter-test-verifier-123456";
        String code = tokenService.createAuthorizationCode(
                new OAuthTokenService.AuthorizationRequest(
                        "quantum-local-agent",
                        "http://127.0.0.1/callback",
                        PkceUtil.s256Challenge(verifier),
                        PkceUtil.S256,
                        properties.getResource(),
                        Set.of("system.user.read"),
                        "nonce-1"),
                loginUser().setSessionGeneration(generations.open(2L)));
        return tokenService.exchangeAuthorizationCode(
                "quantum-local-agent",
                "http://127.0.0.1/callback",
                code,
                verifier,
                properties.getResource());
    }

    private static LoginUser loginUser() {
        return new LoginUser()
                .setUserId(2L)
                .setUsername("alice")
                .setStatus(1)
                .setPermissions(Set.of("system:user:list"));
    }
}
