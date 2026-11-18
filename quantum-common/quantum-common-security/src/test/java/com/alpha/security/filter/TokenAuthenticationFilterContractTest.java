package com.alpha.security.filter;

import com.alpha.security.config.SecurityProperties;
import com.alpha.security.token.TokenService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class TokenAuthenticationFilterContractTest {
    @Test
    void ordinaryRequestsIgnoreARefreshCookie() throws Exception {
        var tokens = mock(TokenService.class);
        var request = new MockHttpServletRequest("GET", "/system/user/list");
        request.setCookies(new Cookie("refresh_token", "test-refresh-cookie"));
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        new TokenAuthenticationFilter(tokens, new SecurityProperties()).doFilter(request, response, chain);

        verifyNoInteractions(tokens);
        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getHeaders("Set-Cookie")).isEmpty();
    }
}
