package com.alpha.mcp.oauth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PkceUtilTest {

    @Test
    void s256ChallengeMatchesRfc7636Vector() {
        String verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";

        String challenge = PkceUtil.s256Challenge(verifier);

        assertThat(challenge).isEqualTo("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM");
        assertThat(PkceUtil.matchesS256(verifier, challenge)).isTrue();
        assertThat(PkceUtil.matchesS256("wrong-verifier", challenge)).isFalse();
    }
}
