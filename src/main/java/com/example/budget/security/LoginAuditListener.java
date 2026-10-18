package com.example.budget.security;

import com.example.budget.mapper.AuditMapper;
import com.example.budget.model.AppUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoginAuditListener {
    private final AuditMapper auditMapper;

    @EventListener
    public void onLogin(InteractiveAuthenticationSuccessEvent event) {
        if (event.getAuthentication().getPrincipal() instanceof AppUserPrincipal user) {
            auditMapper.insert(user.getTenantId(), user.getId(), "LOGIN_SUCCESS", "SESSION", null, "ログイン成功");
        }
    }
}
