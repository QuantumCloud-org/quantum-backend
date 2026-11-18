package com.alpha.security.util;

import com.alpha.framework.constant.CommonConstants;
import jakarta.servlet.http.HttpServletRequest;

public final class BearerTokens {

    private BearerTokens() {
    }

    /** Extracts the token from an HTTP Bearer authorization header. */
    public static String resolve(HttpServletRequest request) {
        String header = request.getHeader(CommonConstants.HEADER_AUTHORIZATION);
        String prefix = CommonConstants.TOKEN_PREFIX;
        if (header == null || !header.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return null;
        }
        String token = header.substring(prefix.length()).strip();
        return token.isEmpty() ? null : token;
    }
}
