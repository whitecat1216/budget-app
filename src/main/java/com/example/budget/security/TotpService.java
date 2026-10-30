package com.example.budget.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;

/** RFC 6238 TOTP with a 30 second step and six digits. */
public final class TotpService {
    private static final char[] BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    public String newSecret() {
        byte[] bytes = new byte[20];
        RANDOM.nextBytes(bytes);
        return encodeBase32(bytes);
    }

    public boolean verify(String secret, String code) {
        if (secret == null || code == null || !code.matches("\\d{6}")) return false;
        long step = Instant.now().getEpochSecond() / 30;
        for (long offset = -1; offset <= 1; offset++) {
            if (java.security.MessageDigest.isEqual(code.getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                    codeAt(secret, (step + offset) * 30).getBytes(java.nio.charset.StandardCharsets.US_ASCII))) return true;
        }
        return false;
    }

    public String provisioningUri(String secret, String email) {
        String label = java.net.URLEncoder.encode("NEXUS:" + email, java.nio.charset.StandardCharsets.UTF_8);
        String issuer = java.net.URLEncoder.encode("NEXUS", java.nio.charset.StandardCharsets.UTF_8);
        return "otpauth://totp/" + label + "?secret=" + secret + "&issuer=" + issuer + "&algorithm=SHA1&digits=6&period=30";
    }

    String codeAt(String secret, long epochSeconds) {
        try {
            byte[] key = decodeBase32(secret);
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(Long.BYTES).putLong(epochSeconds / 30).array());
            int offset = hash[hash.length - 1] & 0x0f;
            int binary = ((hash[offset] & 0x7f) << 24) | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8) | (hash[offset + 3] & 0xff);
            return String.format(java.util.Locale.ROOT, "%06d", binary % 1_000_000);
        } catch (Exception e) {
            throw new IllegalArgumentException("MFA secret is invalid", e);
        }
    }

    private static String encodeBase32(byte[] data) {
        StringBuilder out = new StringBuilder((data.length * 8 + 4) / 5);
        int buffer = 0, bits = 0;
        for (byte value : data) {
            buffer = (buffer << 8) | (value & 0xff);
            bits += 8;
            while (bits >= 5) { out.append(BASE32[(buffer >> (bits - 5)) & 31]); bits -= 5; }
        }
        if (bits > 0) out.append(BASE32[(buffer << (5 - bits)) & 31]);
        return out.toString();
    }

    private static byte[] decodeBase32(String value) {
        String normalized = value.replace("=", "").toUpperCase(java.util.Locale.ROOT);
        byte[] result = new byte[normalized.length() * 5 / 8];
        int buffer = 0, bits = 0, index = 0;
        for (char c : normalized.toCharArray()) {
            int digit = c >= 'A' && c <= 'Z' ? c - 'A' : c >= '2' && c <= '7' ? c - '2' + 26 : -1;
            if (digit < 0) throw new IllegalArgumentException("Invalid Base32 secret");
            buffer = (buffer << 5) | digit; bits += 5;
            if (bits >= 8) { result[index++] = (byte) (buffer >> (bits - 8)); bits -= 8; }
        }
        return Arrays.copyOf(result, index);
    }
}
