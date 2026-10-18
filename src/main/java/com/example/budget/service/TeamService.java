package com.example.budget.service;

import com.example.budget.mapper.RegistrationMapper;
import com.example.budget.mapper.UserMapper;
import com.example.budget.model.InvitationForm;
import com.example.budget.model.TeamUser;
import com.example.budget.security.TenantSession;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TeamService {
    private static final Set<String> ALLOWED_ROLES = Set.of("EXECUTIVE", "FINANCE_ADMIN", "DEPARTMENT_MANAGER", "PROJECT_MANAGER", "APPROVER", "MEMBER", "AUDITOR");
    private final UserMapper userMapper;
    private final RegistrationMapper registrationMapper;
    private final PasswordEncoder passwordEncoder;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final AuditService auditService;
    private final SecureRandom random = new SecureRandom();
    @Value("${app.mail.from:no-reply@localhost}") private String fromAddress;
    @Value("${app.public-url:http://localhost:8080}") private String publicUrl;

    public List<Map<String,Object>> list() { return userMapper.findTeam(TenantSession.tenantId()); }

    @Transactional
    public void invite(InvitationForm form) {
        if (!ALLOWED_ROLES.contains(form.getRole())) throw new IllegalArgumentException("指定されたロールは利用できません。");
        long tenantId = TenantSession.tenantId();
        String email = form.getEmail().trim().toLowerCase();
        byte[] secret = new byte[32]; random.nextBytes(secret);
        TeamUser user = new TeamUser(); user.setTenantId(tenantId); user.setEmail(email);
        user.setDisplayName(form.getDisplayName().trim()); user.setRole(form.getRole());
        user.setPasswordHash(passwordEncoder.encode(HexFormat.of().formatHex(secret)));
        userMapper.insertInvite(user);
        String token = HexFormat.of().formatHex(secret);
        registrationMapper.insertVerification(user.getId(), sha256(token), LocalDateTime.now().plusHours(24), "INVITE");
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) throw new IllegalStateException("メール送信設定がありません。SPRING_MAIL_HOST等を設定してください。");
        SimpleMailMessage mail = new SimpleMailMessage(); mail.setFrom(fromAddress); mail.setTo(email);
        mail.setSubject("予算管理サービスへの招待");
        mail.setText("招待を受けるには24時間以内にリンクを開き、パスワードを設定してください。\n\n" + publicUrl + "/accept-invitation?token=" + token);
        sender.send(mail);
        auditService.record("INVITE", "USER", user.getId(), "ユーザー招待を送信 (" + form.getRole() + ")");
    }

    @Transactional
    public boolean accept(String token, String password) {
        if (token == null || !token.matches("[a-fA-F0-9]{64}") || password == null || password.length() < 12 || password.length() > 72) return false;
        var target = registrationMapper.findValidVerification(sha256(token), "INVITE");
        if (target == null) return false;
        if (registrationMapper.consumeVerification(target.getVerificationId(), "INVITE") != 1) return false;
        if (registrationMapper.activateInvitation(target.getUserId(), target.getTenantId(), passwordEncoder.encode(password)) != 1) return false;
        auditService.recordFor(target.getTenantId(), target.getUserId(), "INVITATION_ACCEPTED", "USER", target.getUserId().toString(), "ユーザー招待を受諾");
        return true;
    }

    private String sha256(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException("Token hashing unavailable", e); }
    }
}
