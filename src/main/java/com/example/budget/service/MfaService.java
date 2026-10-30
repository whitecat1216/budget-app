package com.example.budget.service;

import com.example.budget.mapper.UserMapper;
import com.example.budget.model.AppUserPrincipal;
import com.example.budget.security.TenantSession;
import com.example.budget.security.TotpService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class MfaService {
    public static final String PENDING_SECRET = "MFA_PENDING_SECRET";
    private static final SecureRandom RANDOM = new SecureRandom();
    private final UserMapper users;
    private final TotpService totp = new TotpService();
    @Value("${app.security.mfa-encryption-key:}") private String encodedEncryptionKey;

    public boolean encryptionConfigured() { return encryptionKey() != null; }
    public boolean isEnabled() { return users.mfaEnabled(TenantSession.currentUser().getTenantId(), TenantSession.currentUser().getId()); }

    public String beginEnrollment() {
        requireEncryptionKey();
        return totp.newSecret();
    }

    public String provisioningUri(String secret) {
        return totp.provisioningUri(secret, TenantSession.currentUser().getEmail());
    }

    @Transactional
    public boolean confirmEnrollment(String secret, String code) {
        requireEncryptionKey();
        if (!totp.verify(secret, code)) return false;
        var user = TenantSession.currentUser();
        users.enableMfa(user.getTenantId(), user.getId(), encrypt(secret));
        users.bumpAuthVersion(user.getTenantId(), user.getId());
        return true;
    }

    public boolean verifyCurrentCode(String code) {
        var user = TenantSession.currentUser();
        String encrypted = users.mfaSecret(user.getTenantId(), user.getId());
        if (encrypted == null) return false;
        try { return totp.verify(decrypt(encrypted), code); }
        catch (RuntimeException e) { return false; }
    }

    @Transactional
    public boolean disable(String code) {
        if (!verifyCurrentCode(code)) return false;
        var user = TenantSession.currentUser();
        users.disableMfa(user.getTenantId(), user.getId());
        users.bumpAuthVersion(user.getTenantId(), user.getId());
        return true;
    }

    public boolean verifySecret(String secret, String code) { return totp.verify(secret, code); }

    private String encrypt(String plainText) {
        try {
            byte[] iv = new byte[12]; RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(requireEncryptionKey(), "AES"), new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array());
        } catch (Exception e) { throw new IllegalStateException("MFA secret encryption failed", e); }
    }

    private String decrypt(String encoded) {
        try {
            byte[] payload = Base64.getDecoder().decode(encoded);
            byte[] iv = java.util.Arrays.copyOfRange(payload, 0, 12);
            byte[] encrypted = java.util.Arrays.copyOfRange(payload, 12, payload.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(requireEncryptionKey(), "AES"), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) { throw new IllegalStateException("MFA secret decryption failed", e); }
    }

    private byte[] encryptionKey() {
        if (encodedEncryptionKey == null || encodedEncryptionKey.isBlank()) return null;
        try {
            byte[] key = Base64.getDecoder().decode(encodedEncryptionKey);
            return key.length == 32 ? key : null;
        } catch (IllegalArgumentException e) { return null; }
    }

    private byte[] requireEncryptionKey() {
        byte[] key = encryptionKey();
        if (key == null) throw new IllegalStateException("MFA_ENCRYPTION_KEY にBase64形式の32バイト鍵を設定してください。");
        return key;
    }
}
