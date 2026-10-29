package com.example.budget.service;

import com.example.budget.mapper.SpendingRequestMapper;
import com.example.budget.mapper.OrganizationMapper;
import com.example.budget.model.SpendingRequest;
import com.example.budget.security.TenantSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SpendingRequestService {
    private final SpendingRequestMapper mapper;
    private final OrganizationMapper organizationMapper;
    private final CategoryService categoryService;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public List<SpendingRequest> list() {
        var user = TenantSession.currentUser();
        boolean canSeeAll = user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_TENANT_ADMIN") || a.getAuthority().equals("ROLE_FINANCE_ADMIN") || a.getAuthority().equals("ROLE_APPROVER"));
        var allVisible = mapper.findAll(user.getTenantId(), user.getId(), canSeeAll || user.getRole().equals("DEPARTMENT_MANAGER") || user.getRole().equals("PROJECT_MANAGER"));
        return allVisible.stream().filter(r -> r.getRequesterId().equals(user.getId()) || TenantSession.canAccess(r.getDepartmentId(), r.getProjectId())).toList();
    }

    @Transactional
    public void create(SpendingRequest request) {
        var user = TenantSession.currentUser();
        validateDimensions(request, user.getTenantId());
        TenantSession.requireScope(request.getDepartmentId(), request.getProjectId());
        request.setTenantId(user.getTenantId()); request.setRequesterId(user.getId());
        mapper.insert(request);
        auditService.record("CREATE", "SPENDING_REQUEST", request.getId(), "支出申請を作成");
    }

    @Transactional
    public void submit(long id) {
        var user = TenantSession.currentUser();
        SpendingRequest current = mapper.findById(id, user.getTenantId());
        if (current == null) throw new IllegalArgumentException("申請が見つかりません。");
        TenantSession.requireScope(current.getDepartmentId(), current.getProjectId());
        if (mapper.submit(id, user.getTenantId(), user.getId()) != 1) throw new IllegalStateException("提出できる状態の申請ではありません。");
        mapper.insertHistory(user.getTenantId(), id, user.getId(), "SUBMITTED", null);
        auditService.record("SUBMIT", "SPENDING_REQUEST", id, "支出申請を提出");
        notificationService.notifyReviewers(user.getTenantId(), user.getId(), current.getDepartmentId(),
                current.getProjectId(), id, current.getTitle());
    }

    @Transactional
    public void review(long id, String action, String comment) {
        var user = TenantSession.currentUser();
        SpendingRequest request = mapper.findById(id, user.getTenantId());
        if (request == null) throw new IllegalArgumentException("申請が見つかりません。");
        if (request.getRequesterId().equals(user.getId())) throw new IllegalStateException("自分の申請は承認できません。");
        TenantSession.requireScope(request.getDepartmentId(), request.getProjectId());
        int updated = switch (action) {
            case "APPROVED" -> mapper.approve(id, user.getTenantId(), user.getId());
            case "RETURNED", "REJECTED" -> mapper.finishReview(id, user.getTenantId(), user.getId(), action);
            default -> 0;
        };
        if (updated != 1) throw new IllegalStateException("他の操作で状態が変わったか、承認対象ではありません。");
        mapper.insertHistory(user.getTenantId(), id, user.getId(), action, comment);
        auditService.record(action, "SPENDING_REQUEST", id, "支出申請を" + action + "");
        notificationService.notifyRequester(user.getTenantId(), request.getRequesterId(), id, request.getTitle(), action);
    }

    public List<java.util.Map<String,Object>> history(long id) {
        long tenantId = TenantSession.tenantId();
        if (mapper.findById(id, tenantId) == null) throw new IllegalArgumentException("申請が見つかりません。");
        return mapper.findHistory(tenantId, id);
    }

    private void validateDimensions(SpendingRequest request, long tenantId) {
        if (request.getCategoryId() != null && categoryService.findById(request.getCategoryId()) == null) throw new IllegalArgumentException("カテゴリを確認してください。");
        if (request.getDepartmentId() != null && !organizationMapper.departmentBelongsToTenant(request.getDepartmentId(), tenantId)) throw new IllegalArgumentException("部門を確認してください。");
        if (request.getProjectId() != null && !organizationMapper.projectBelongsToTenant(request.getProjectId(), tenantId)) throw new IllegalArgumentException("プロジェクトを確認してください。");
        if (request.getProjectId() != null) {
            Long projectDepartmentId = organizationMapper.findProjectDepartmentId(request.getProjectId(), tenantId);
            if (projectDepartmentId != null && request.getDepartmentId() == null) request.setDepartmentId(projectDepartmentId);
            else if (projectDepartmentId != null && !projectDepartmentId.equals(request.getDepartmentId())) throw new IllegalArgumentException("プロジェクトに紐づく部門を選択してください。");
        }
    }
}
