package com.example.budget.security;

import com.example.budget.model.AppUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class TenantSession {
    private TenantSession() { }
    public static AppUserPrincipal currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserPrincipal principal)) {
            throw new IllegalStateException("認証済みユーザーが必要です");
        }
        return principal;
    }
    public static long tenantId() { return currentUser().getTenantId(); }
}
