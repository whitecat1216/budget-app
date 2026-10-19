package com.example.budget.service;

import com.example.budget.mapper.AuditMapper;
import com.example.budget.mapper.RegistrationMapper;
import com.example.budget.mapper.UserMapper;
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
public class PasswordResetService {
    private final UserMapper users;
    private final RegistrationMapper registrations;
    private final AuditMapper audits;
    private final PasswordEncoder encoder;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final SecureRandom random = new SecureRandom();
    @Value("${app.mail.from:no-reply@localhost}") private String from;
    @Value("${app.public-url:http://localhost:8080}") private String publicUrl;

    @Transactional
    public void request(String email) {
        var user = users.findEnabledByEmailForReset(email.trim().toLowerCase());
        if (user == null) return;
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);
        users.insertPasswordReset(user.getId(), sha256(token), LocalDateTime.now().plusMinutes(30));
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) throw new IllegalStateException("メール送信設定がありません。");
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from); message.setTo(email.trim()); message.setSubject("パスワード再設定");
        message.setText("30分以内に次のリンクを開き、新しいパスワードを設定してください。\n\n" + publicUrl + "/reset-password?token=" + token);
        sender.send(message);
    }

    @Transactional
    public boolean reset(String token, String password) {
        if (token == null || !token.matches("[a-fA-F0-9]{64}") || password == null || password.length() < 12 || password.length() > 72) return false;
        var target = users.findValidPasswordReset(sha256(token));
        if (target == null || registrations.consumeVerification(target.getVerificationId(), "PASSWORD_RESET") != 1) return false;
        if (users.updatePassword(target.getUserId(), target.getTenantId(), encoder.encode(password)) != 1) return false;
        audits.insert(target.getTenantId(), target.getUserId(), "PASSWORD_RESET", "USER", target.getUserId().toString(), "パスワードを再設定");
        return true;
    }

    private String sha256(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException("Token hashing unavailable", e); }
    }
}
