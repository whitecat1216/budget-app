package com.example.budget.service;

import com.example.budget.mapper.RegistrationMapper;
import com.example.budget.mapper.CategoryMapper;
import com.example.budget.mapper.AuditMapper;
import com.example.budget.model.RegistrationForm;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RegistrationService {
    private final RegistrationMapper registrationMapper;
    private final CategoryMapper categoryMapper;
    private final AuditMapper auditMapper;
    private final PasswordEncoder passwordEncoder;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.mail.from:no-reply@localhost}") private String fromAddress;
    @Value("${app.public-url:http://localhost:8080}") private String publicUrl;

    @Transactional
    public void register(RegistrationForm form) {
        if (!form.isTermsAccepted()) throw new IllegalArgumentException("利用規約への同意が必要です。");
        String email = form.getEmail().trim().toLowerCase();
        Long tenantId = insertTenant(form.getCompanyName().trim());
        Long userId = insertUser(tenantId, email, form.getDisplayName().trim(), passwordEncoder.encode(form.getPassword()));
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String token = HexFormat.of().formatHex(tokenBytes);
        registrationMapper.insertVerification(userId, sha256(token), LocalDateTime.now().plusMinutes(30), "REGISTRATION");
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) throw new IllegalStateException("メール送信設定がありません。SPRING_MAIL_HOST等を設定してください。");
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("予算管理サービス メールアドレスの確認");
        message.setText("次のリンクを30分以内に開いて、企業登録を完了してください。\n\n" + publicUrl + "/verify-email?token=" + token);
        sender.send(message);
    }

    @Transactional
    public boolean verify(String token) {
        if (token == null || !token.matches("[a-fA-F0-9]{64}")) return false;
        var target = registrationMapper.findValidVerification(sha256(token), "REGISTRATION");
        if (target == null) return false;
        if (registrationMapper.consumeVerification(target.getVerificationId(), "REGISTRATION") != 1) return false;
        if (registrationMapper.enableUser(target.getUserId()) != 1) return false;
        if (registrationMapper.activateTenant(target.getTenantId()) != 1) return false;
        categoryMapper.copyDefaultCategories(target.getTenantId());
        auditMapper.insert(target.getTenantId(), target.getUserId(), "EMAIL_VERIFIED", "TENANT", target.getTenantId().toString(), "企業登録メールを確認");
        return true;
    }

    private Long insertTenant(String name) {
        TenantKey key = new TenantKey(name);
        registrationMapper.insertTenantKey(key);
        return key.getTenantId();
    }

    private Long insertUser(Long tenantId, String email, String name, String passwordHash) {
        UserKey key = new UserKey(tenantId, email, name, passwordHash);
        registrationMapper.insertUserKey(key);
        return key.getUserId();
    }

    private String sha256(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException("Token hashing unavailable", e); }
    }

    public static class TenantKey {
        private final String companyName; private Long tenantId;
        TenantKey(String companyName) { this.companyName = companyName; }
        public String getCompanyName() { return companyName; } public Long getTenantId() { return tenantId; } public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    }
    public static class UserKey {
        private final Long tenantId; private final String email; private final String displayName; private final String passwordHash; private Long userId;
        UserKey(Long tenantId, String email, String displayName, String passwordHash) { this.tenantId=tenantId; this.email=email; this.displayName=displayName; this.passwordHash=passwordHash; }
        public Long getTenantId(){return tenantId;} public String getEmail(){return email;} public String getDisplayName(){return displayName;} public String getPasswordHash(){return passwordHash;} public Long getUserId(){return userId;} public void setUserId(Long userId){this.userId=userId;}
    }
}
