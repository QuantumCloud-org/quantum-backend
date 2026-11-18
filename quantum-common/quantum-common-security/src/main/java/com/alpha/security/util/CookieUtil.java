package com.alpha.security.util;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

/**
 * Refresh Token Cookie 工具。
 */
public final class CookieUtil {

    public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    private static final String LEGACY_PATH = "/";

    private CookieUtil() {
    }

    public static void writeRefreshCookie(HttpServletResponse response, String token, boolean persistent,
                                          int maxAgeSeconds, boolean secure, String path) {
        if (LEGACY_PATH.equals(path)) {
            writeCookie(response, "", 0, secure, LEGACY_PATH);
        }
        writeCookie(response, token, persistent ? maxAgeSeconds : -1, secure, path);
        if (!LEGACY_PATH.equals(path)) {
            writeCookie(response, "", 0, secure, LEGACY_PATH);
        }
    }

    public static String readRefreshCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName()) && StrUtil.isNotBlank(cookie.getValue())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public static void clearRefreshCookie(HttpServletResponse response, boolean secure, String path) {
        writeCookie(response, "", 0, secure, path);
        if (!LEGACY_PATH.equals(path)) {
            writeCookie(response, "", 0, secure, LEGACY_PATH);
        }
    }

    private static void writeCookie(HttpServletResponse response, String value, int maxAge, boolean secure, String path) {
        var cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, value)
                .httpOnly(true).sameSite("Strict").secure(secure).path(path).maxAge(maxAge).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
