package com.alpha.security.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class CookieUtilTest {

    @Test
    void writeRefreshCookieShouldIncludeSecureWhenEnabled() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        CookieUtil.writeRefreshCookie(response, "token-1", true, 3600, true);

        assertThat(response.getHeader("Set-Cookie"))
                .contains("refresh_token=token-1")
                .contains("HttpOnly")
                .contains("SameSite=Strict")
                .contains("Secure")
                .contains("Max-Age=3600");
    }

    @Test
    void writeRefreshCookieShouldOmitSecureWhenDisabled() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        CookieUtil.writeRefreshCookie(response, "token-1", false, 3600, false);

        assertThat(response.getHeader("Set-Cookie"))
                .contains("refresh_token=token-1")
                .doesNotContain("Secure")
                .doesNotContain("Max-Age=");
    }

    @Test
    void clearRefreshCookieShouldRespectSecureFlag() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        CookieUtil.clearRefreshCookie(response, true);

        assertThat(response.getHeader("Set-Cookie"))
                .contains("refresh_token=")
                .contains("Max-Age=0")
                .contains("Secure");
    }
}
