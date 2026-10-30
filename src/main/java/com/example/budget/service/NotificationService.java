package com.example.budget.service;

import com.example.budget.mapper.NotificationMapper;
import com.example.budget.security.TenantSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationMapper mapper;

    public List<Map<String, Object>> recent() {
        var user = TenantSession.currentUser();
        return mapper.findRecent(user.getTenantId(), user.getId());
    }

    public long unreadCount() {
        var user = TenantSession.currentUser();
        return mapper.countUnread(user.getTenantId(), user.getId());
    }

    @Transactional
    public void notifyReviewers(long tenantId, long requesterId, Long departmentId, Long projectId,
                                long requestId, String requestTitle) {
        notifyRoleReviewers(tenantId, requesterId, departmentId, projectId, requestId, requestTitle, "ANY_APPROVER");
    }

    @Transactional
    public void notifyRoleReviewers(long tenantId, long requesterId, Long departmentId, Long projectId,
                                    long requestId, String requestTitle, String requiredRole) {
        String shortTitle = requestTitle == null ? "支出申請" : requestTitle.substring(0, Math.min(requestTitle.length(), 120));
        mapper.insertForReviewers(tenantId, requesterId, departmentId, projectId, requiredRole, "APPROVAL_REQUIRED",
                "支出申請の確認", "「" + shortTitle + "」の確認が必要です。", "/requests#request-" + requestId);
    }

    @Transactional
    public void notifyRequester(long tenantId, long requesterId, long requestId, String requestTitle, String action) {
        String status = switch (action) {
            case "APPROVED" -> "承認されました";
            case "RETURNED" -> "差し戻されました";
            case "REJECTED" -> "却下されました";
            default -> "更新されました";
        };
        String shortTitle = requestTitle == null ? "支出申請" : requestTitle.substring(0, Math.min(requestTitle.length(), 120));
        mapper.insertForRecipient(tenantId, requesterId, "REQUEST_" + action, "申請状況のお知らせ",
                "「" + shortTitle + "」が" + status + "。", "/requests#request-" + requestId);
    }

    @Transactional
    public void markRead(long id) {
        var user = TenantSession.currentUser();
        mapper.markRead(id, user.getTenantId(), user.getId());
    }

    @Transactional
    public void markAllRead() {
        var user = TenantSession.currentUser();
        mapper.markAllRead(user.getTenantId(), user.getId());
    }
}
