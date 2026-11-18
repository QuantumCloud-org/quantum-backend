package com.alpha.system.controller;

import com.alpha.cache.util.LocalCacheClient;
import com.alpha.framework.config.JacksonConfig;
import com.alpha.framework.entity.LoginUser;
import com.alpha.framework.util.ServletJsonWriter;
import com.alpha.system.dto.response.LoginResponse;
import com.alpha.security.config.SecurityProperties;
import com.alpha.security.handler.LogoutSuccessHandlerImpl;
import com.alpha.security.service.ICaptchaService;
import com.alpha.security.token.SessionGeneration;
import com.alpha.security.token.TokenInfo;
import com.alpha.security.token.TokenProperties;
import com.alpha.security.token.TokenService;
import com.alpha.system.convert.UserConvert;
import com.alpha.system.service.ISysUserService;
import com.alpha.system.service.impl.LoginServiceImpl;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RefreshContractTest {
    private static final String TEST_SIGNING_KEY = "test-fake-signing-key-".repeat(4);
    private TokenService tokens;
    private MockMvc mvc;
    private UserDetailsService users;
    private LoginServiceImpl loginService;
    private SessionGeneration generations;

    @BeforeEach
    void setup() {
        var tokenProperties = new TokenProperties();
        tokenProperties.setSecret(TEST_SIGNING_KEY);
        users = mock(UserDetailsService.class);
        when(users.loadUserByUsername("alice")).thenAnswer(invocation -> loginUser());
        var cache = new LocalCacheClient();
        generations = new SessionGeneration(cache);
        tokens = new TokenService(cache, tokenProperties, users, generations);
        var security = new SecurityProperties();
        security.setRefreshCookieSecure(true);
        loginService = mock(LoginServiceImpl.class);
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(loginService,
                        mock(ICaptchaService.class), mock(ISysUserService.class), new UserConvert(), tokens, security))
                .setMessageConverters(new JacksonJsonHttpMessageConverter(new JacksonConfig().jsonMapper()))
                .build();
    }

    @Test
    void rotatesCookieAndReturnsAccessTokenAndItsLifetime() throws Exception {
        TokenInfo old = issue();
        var response = mvc.perform(post("/auth/refresh").cookie(new Cookie("refresh_token", old.getRefreshToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.expiresIn").value(1800))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andReturn().getResponse();

        assertThat(response.getHeader("Authorization")).startsWith("Bearer ").doesNotContain(old.getAccessToken());
        assertThat(response.getHeaders("Set-Cookie")).hasSize(2)
                .anySatisfy(header -> assertThat(header).contains("Path=/api/auth", "HttpOnly", "SameSite=Strict", "Secure")
                        .doesNotContain("Max-Age=0", old.getRefreshToken()))
                .anySatisfy(header -> assertThat(header).contains("refresh_token=;", "Path=/;", "Max-Age=0"));
    }

    @Test
    void missingCookieReturnsUnauthorizedAndClearsBothPaths() throws Exception {
        var response = mvc.perform(post("/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andReturn().getResponse();
        assertThat(response.getHeaders("Set-Cookie")).hasSize(2)
                .allSatisfy(header -> assertThat(header).contains("refresh_token=;", "Max-Age=0"));
    }

    @Test
    void revokedTokenReturnsUnauthorizedAndClearsBothPaths() throws Exception {
        TokenInfo old = issue();
        tokens.logoutByTokenId(old.getTokenId());
        var response = mvc.perform(post("/auth/refresh").cookie(new Cookie("refresh_token", old.getRefreshToken())))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse();
        assertThat(response.getHeaders("Set-Cookie")).hasSize(2)
                .allSatisfy(header -> assertThat(header).contains("Max-Age=0"));
    }

    @Test
    void loginAndRefreshReturnTheSameSessionIdentityAndNewLoginChangesIt() throws Exception {
        TokenInfo first = issue();
        when(loginService.login(any())).thenReturn(LoginResponse.of(loginUser(), first));
        var login = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","password":"test-password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").isNotEmpty())
                .andReturn().getResponse();
        var mapper = new JacksonConfig().jsonMapper();
        String sessionId = mapper.readTree(login.getContentAsString()).path("data").path("sessionId").asString();

        mvc.perform(post("/auth/refresh").cookie(new Cookie("refresh_token", first.getRefreshToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId));

        when(loginService.login(any())).thenReturn(LoginResponse.of(loginUser(), issue()));
        var next = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","password":"test-password"}
                                """))
                .andReturn().getResponse();
        assertThat(mapper.readTree(next.getContentAsString()).path("data").path("sessionId").asString())
                .isNotBlank().isNotEqualTo(sessionId);
    }

    @Test
    void obsoleteRefreshCannotOverwriteRotatedCookieInEitherResponseOrder() throws Exception {
        TokenInfo old = issue();
        Cookie cookie = new Cookie("refresh_token", old.getRefreshToken());
        var winner = mvc.perform(post("/auth/refresh").cookie(cookie))
                .andExpect(status().isOk()).andReturn().getResponse();
        var loser = mvc.perform(post("/auth/refresh").cookie(cookie))
                .andExpect(status().isConflict()).andReturn().getResponse();

        assertThat(loser.getHeaders("Set-Cookie")).isEmpty();
        assertThat(applyCookies(old.getRefreshToken(), winner, loser))
                .isNotEqualTo(old.getRefreshToken())
                .isEqualTo(applyCookies(old.getRefreshToken(), loser, winner));
        assertThat(tokens.validateToken(old.getAccessToken())).isNotNull();
    }

    @Test
    void userStoreFailurePreservesCookieAndRefreshCanBeRetried() throws Exception {
        Cookie cookie = new Cookie("refresh_token", issue().getRefreshToken());
        when(users.loadUserByUsername("alice")).thenThrow(new DataAccessResourceFailureException("unavailable"))
                .thenAnswer(invocation -> loginUser());

        var response = mvc.perform(post("/auth/refresh").cookie(cookie))
                .andExpect(status().isServiceUnavailable()).andReturn().getResponse();
        assertThat(response.getHeaders("Set-Cookie")).isEmpty();
        mvc.perform(post("/auth/refresh").cookie(cookie)).andExpect(status().isOk());
    }

    @Test
    void expiredSupersededRefreshLeavesWinningCookieIntact() throws Exception {
        TokenInfo old = issue();
        var winner = mvc.perform(post("/auth/refresh").cookie(new Cookie("refresh_token", old.getRefreshToken())))
                .andExpect(status().isOk()).andReturn().getResponse();
        var delayed = mvc.perform(post("/auth/refresh")
                        .cookie(new Cookie("refresh_token", expired(old.getRefreshToken()))))
                .andExpect(status().isConflict()).andReturn().getResponse();
        String fresh = applyCookies(old.getRefreshToken(), winner, delayed);
        mvc.perform(post("/auth/refresh").cookie(new Cookie("refresh_token", fresh))).andExpect(status().isOk());
    }

    @Test
    void logoutFromAnOldPagePreservesTheDifferentSessionsCookie() throws Exception {
        var old = issue();
        var other = new LoginUser().setUserId(3L).setUsername("bob").setStatus(1);
        var current = tokens.createToken(other, generations.open(3L), null, null, null, true);
        var handler = new LogoutSuccessHandlerImpl(tokens, new ServletJsonWriter(new JacksonConfig().jsonMapper()),
                new SecurityProperties());
        var request = new MockHttpServletRequest("POST", "/auth/logout");
        request.addHeader("Authorization", "Bearer " + expired(old.getAccessToken()));
        request.setCookies(new Cookie("refresh_token", current.getRefreshToken()));
        var response = new MockHttpServletResponse();

        handler.onLogoutSuccess(request, response, null);

        assertThat(response.getHeaders("Set-Cookie")).isEmpty();
        assertThat(tokens.validateToken(current.getAccessToken())).isNotNull();
    }

    private static String expired(String compact) throws Exception {
        var original = SignedJWT.parse(compact);
        var claims = new JWTClaimsSet.Builder(original.getJWTClaimsSet())
                .expirationTime(Date.from(Instant.now().minusSeconds(1))).build();
        var expired = new SignedJWT(original.getHeader(), claims);
        expired.sign(new MACSigner(TEST_SIGNING_KEY.getBytes(StandardCharsets.UTF_8)));
        return expired.serialize();
    }

    private static String applyCookies(String current, MockHttpServletResponse... responses) {
        for (var response : responses) {
            for (String header : response.getHeaders("Set-Cookie")) {
                if (header.startsWith("refresh_token=") && header.contains("Path=/api/auth")) {
                    current = header.substring("refresh_token=".length(), header.indexOf(';'));
                }
            }
        }
        return current;
    }

    private TokenInfo issue() throws Exception {
        return tokens.createToken(loginUser(), generations.open(2L), null, null, null, true);
    }

    private static LoginUser loginUser() {
        return new LoginUser().setUserId(2L).setUsername("alice").setStatus(1);
    }
}
