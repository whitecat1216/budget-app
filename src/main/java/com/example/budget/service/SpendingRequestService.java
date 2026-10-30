package com.example.budget.service;

import com.example.budget.mapper.SpendingRequestMapper;
import com.example.budget.mapper.OrganizationMapper;
import com.example.budget.mapper.ApprovalWorkflowMapper;
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
    private final ApprovalPolicyService approvalPolicyService;
    private final ApprovalWorkflowMapper approvalWorkflowMapper;

    public List<SpendingRequest> list() {
        var user = TenantSession.currentUser();
        boolean canSeeAll = user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_TENANT_ADMIN") || a.getAuthority().equals("ROLE_FINANCE_ADMIN") || a.getAuthority().equals("ROLE_APPROVER"));
        var allVisible = mapper.findAll(user.getTenantId(), user.getId(), canSeeAll || user.getRole().equals("DEPARTMENT_MANAGER") || user.getRole().equals("PROJECT_MANAGER"));
        return allVisible.stream().filter(r -> r.getRequesterId().equals(user.getId()) || TenantSession.canAccess(r.getDepartmentId(), r.getProjectId()))
                .peek(r -> r.setCanReview("SUBMITTED".equals(r.getStatus()) &&
                        ("ANY_APPROVER".equals(r.getCurrentApprovalRole()) || user.getRole().equals(r.getCurrentApprovalRole()) ||
                                (r.getCurrentApprovalRole() == null && r.getApprovalRound() == 0))))
                .toList();
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
        int round = mapper.approvalRound(id, user.getTenantId());
        List<String> route = approvalPolicyService.rolesForAmount(user.getTenantId(), current.getAmount());
        approvalWorkflowMapper.insertStages(user.getTenantId(), id, round, route);
        mapper.insertHistory(user.getTenantId(), id, user.getId(), "SUBMITTED", null);
        auditService.record("SUBMIT", "SPENDING_REQUEST", id, "支出申請を提出");
        notificationService.notifyRoleReviewers(user.getTenantId(), user.getId(), current.getDepartmentId(),
                current.getProjectId(), id, current.getTitle(), route.get(0));
    }

    @Transactional
    public void review(long id, String action, String comment) {
        var user = TenantSession.currentUser();
        SpendingRequest request = mapper.findById(id, user.getTenantId());
        if (request == null) throw new IllegalArgumentException("申請が見つかりません。");
        if (request.getRequesterId().equals(user.getId())) throw new IllegalStateException("自分の申請は承認できません。");
        TenantSession.requireScope(request.getDepartmentId(), request.getProjectId());
        if (!"SUBMITTED".equals(request.getStatus())) throw new IllegalStateException("承認対象ではありません。");
        String requiredRole = approvalWorkflowMapper.currentRole(user.getTenantId(), id,
                request.getApprovalRound(), request.getCurrentApprovalStep());
        if (requiredRole == null && request.getApprovalRound() == 0) requiredRole = "ANY_APPROVER";
        if (requiredRole == null || !"ANY_APPROVER".equals(requiredRole) && !user.getRole().equals(requiredRole))
            throw new org.springframework.security.access.AccessDeniedException("現在の承認段階を処理する権限がありません。");
        if ("ANY_APPROVER".equals(requiredRole) && !List.of("TENANT_ADMIN", "FINANCE_ADMIN", "APPROVER", "DEPARTMENT_MANAGER", "PROJECT_MANAGER").contains(user.getRole()))
            throw new org.springframework.security.access.AccessDeniedException("承認権限がありません。");

        boolean finalApproval = true;
        if ("APPROVED".equals(action)) {
            if (approvalWorkflowMapper.hasStep(user.getTenantId(), id, request.getApprovalRound(), request.getCurrentApprovalStep())) {
                if (approvalWorkflowMapper.approveStage(user.getTenantId(), id, request.getApprovalRound(), request.getCurrentApprovalStep(), user.getId(), requiredRole) != 1)
                    throw new IllegalStateException("同じ申請の別段階を承認済みか、他の操作で状態が変わりました。");
                int nextStep = request.getCurrentApprovalStep() + 1;
                if (approvalWorkflowMapper.hasStep(user.getTenantId(), id, request.getApprovalRound(), nextStep)) {
                    if (approvalWorkflowMapper.advance(user.getTenantId(), id, request.getApprovalRound(), request.getCurrentApprovalStep(), nextStep) != 1)
                        throw new IllegalStateException("次の承認段階へ進めませんでした。");
                    String nextRole = approvalWorkflowMapper.currentRole(user.getTenantId(), id, request.getApprovalRound(), nextStep);
                    notificationService.notifyRoleReviewers(user.getTenantId(), request.getRequesterId(), request.getDepartmentId(),
                            request.getProjectId(), id, request.getTitle(), nextRole);
                    finalApproval = false;
                } else if (approvalWorkflowMapper.complete(user.getTenantId(), id, request.getApprovalRound(), request.getCurrentApprovalStep()) != 1) {
                    throw new IllegalStateException("承認を完了できませんでした。");
                }
            } else if (mapper.approve(id, user.getTenantId(), user.getId()) != 1) {
                throw new IllegalStateException("他の操作で状態が変わったか、承認対象ではありません。");
            }
        } else if ("RETURNED".equals(action) || "REJECTED".equals(action)) {
            approvalWorkflowMapper.skipPending(user.getTenantId(), id, request.getApprovalRound());
            if (mapper.finishReview(id, user.getTenantId(), user.getId(), action) != 1)
                throw new IllegalStateException("他の操作で状態が変わったか、承認対象ではありません。");
        } else {
            throw new IllegalArgumentException("処理内容が不正です。");
        }
        String historyAction = "APPROVED".equals(action) && !finalApproval ? "STEP_APPROVED" : action;
        mapper.insertHistory(user.getTenantId(), id, user.getId(), historyAction, comment);
        auditService.record(historyAction, "SPENDING_REQUEST", id, "支出申請を" + historyAction);
        if (!"APPROVED".equals(action) || finalApproval)
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
