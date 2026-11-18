package com.alpha.security.util;

import java.nio.charset.StandardCharsets;

/** BCrypt input requirements shared by authentication and password writes. */
public final class BcryptPasswords {

    public static final int MAX_BYTES = 72;

    private BcryptPasswords() {
    }

    public static boolean withinByteLimit(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES;
    }

    public static boolean isLegacy(String hash) {
        return hash != null && hash.startsWith("$2");
    }
}
