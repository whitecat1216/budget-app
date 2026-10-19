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

    public static boolean isScopeLimited() {
        String role = currentUser().getRole();
        return role.equals("DEPARTMENT_MANAGER") || role.equals("PROJECT_MANAGER");
    }

    public static boolean canAccess(Long departmentId, Long projectId) {
        var user = currentUser();
        return switch (user.getRole()) {
            case "DEPARTMENT_MANAGER" -> departmentId != null && user.getDepartmentScopes().contains(departmentId);
            case "PROJECT_MANAGER" -> projectId != null && user.getProjectScopes().contains(projectId);
            default -> true;
        };
    }

    public static void requireScope(Long departmentId, Long projectId) {
        if (!canAccess(departmentId, projectId)) throw new org.springframework.security.access.AccessDeniedException("対象データへの権限がありません。");
    }
}
