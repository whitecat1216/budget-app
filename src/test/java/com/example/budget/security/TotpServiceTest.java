package com.example.budget.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TotpServiceTest {
    private final TotpService totp = new TotpService();

    @Test
    void matchesRfc6238Sha1SixDigitVector() {
        assertEquals("287082", totp.codeAt("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ", 59));
    }

    @Test
    void generatedSecretIsValidBase32WithoutPadding() {
        String secret = totp.newSecret();
        org.junit.jupiter.api.Assertions.assertTrue(secret.matches("[A-Z2-7]{32}"));
    }
}
