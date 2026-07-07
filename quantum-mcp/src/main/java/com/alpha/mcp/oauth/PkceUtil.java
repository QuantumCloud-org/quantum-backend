package com.alpha.mcp.oauth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public final class PkceUtil {

    public static final String S256 = "S256";

    private PkceUtil() {
    }

    public static String s256Challenge(String codeVerifier) {
        if (codeVerifier == null || codeVerifier.isBlank()) {
            throw new IllegalArgumentException("code_verifier is required");
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    public static boolean matchesS256(String codeVerifier, String expectedChallenge) {
        if (expectedChallenge == null || expectedChallenge.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                s256Challenge(codeVerifier).getBytes(StandardCharsets.US_ASCII),
                expectedChallenge.getBytes(StandardCharsets.US_ASCII)
        );
    }
}
