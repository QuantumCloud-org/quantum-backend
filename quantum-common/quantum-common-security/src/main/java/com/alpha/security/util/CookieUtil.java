package com.alpha.security.util;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Refresh Token Cookie 工具。
 * 当前部署环境为内网 HTTP，因此明确不设置 Secure 标记。
 */
public final class CookieUtil {

    public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    public static final String REFRESH_TOKEN_COOKIE_PATH = "/";

    private CookieUtil() {
    }

    public static void writeRefreshCookie(HttpServletResponse response, String refreshToken, boolean persistent, int maxAgeSeconds) {
        response.addHeader("Set-Cookie", buildRefreshCookieHeader(refreshToken, persistent, maxAgeSeconds));
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

    public static void clearRefreshCookie(HttpServletResponse response) {
        response.addHeader("Set-Cookie", buildRefreshCookieHeader("", true, 0));
    }

    private static String buildRefreshCookieHeader(String value, boolean persistent, int maxAgeSeconds) {
        StringBuilder builder = new StringBuilder();
        builder.append(REFRESH_TOKEN_COOKIE_NAME)
                .append("=")
                .append(value == null ? "" : value)
                .append("; HttpOnly")
                .append("; SameSite=Strict")
                .append("; Path=")
                .append(REFRESH_TOKEN_COOKIE_PATH);

        if (persistent) {
            builder.append("; Max-Age=").append(Math.max(maxAgeSeconds, 0));
        }

        return builder.toString();
    }
}
