package com.example.budget.service;

import com.example.budget.mapper.BudgetChangeRequestMapper;
import com.example.budget.model.BudgetChangeRequest;
import com.example.budget.security.TenantSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetChangeRequestService {
    private final BudgetChangeRequestMapper mapper;
    private final BudgetService budgets;
    private final AuditService audit;

    public List<BudgetChangeRequest> list() {
        return mapper.findAll(TenantSession.tenantId()).stream()
                .filter(r -> TenantSession.canAccess(r.getDepartmentId(), r.getProjectId()))
                .toList();
    }

    @Transactional
    public void create(long budgetId, BigDecimal delta, String reason) {
        if (delta == null || delta.signum() == 0 || reason == null || reason.isBlank() || reason.length() > 1000)
            throw new IllegalArgumentException("増減額と理由を入力してください。");
        var budget = budgets.findById(budgetId);
        if (budget == null) throw new IllegalArgumentException("予算が見つかりません。");
        TenantSession.requireScope(budget.getDepartmentId(), budget.getProjectId());
        if (budget.getAmount().add(delta).signum() < 0) throw new IllegalArgumentException("変更後の予算は0円以上にしてください。");
        BudgetChangeRequest request = new BudgetChangeRequest();
        request.setTenantId(TenantSession.tenantId()); request.setRequesterId(TenantSession.currentUser().getId());
        request.setBudgetId(budgetId); request.setChangeAmount(delta); request.setReason(reason.trim());
        mapper.insert(request);
        audit.record("BUDGET_CHANGE_REQUESTED", "BUDGET_CHANGE_REQUEST", request.getId(), "予算変更申請を作成");
    }

    @Transactional
    public void review(long id, String decision, String comment) {
        if (!"APPROVED".equals(decision) && !"REJECTED".equals(decision)) throw new IllegalArgumentException("無効な審査結果です。");
        long tenantId = TenantSession.tenantId(); long reviewerId = TenantSession.currentUser().getId();
        var request = mapper.findById(id, tenantId);
        if (request == null) throw new IllegalArgumentException("申請が見つかりません。");
        if (!"PENDING".equals(request.getStatus())) throw new IllegalStateException("申請はすでに審査済みです。");
        if (request.getRequesterId().equals(reviewerId)) throw new IllegalStateException("自分の申請は承認できません。");
        var budget = budgets.findById(request.getBudgetId());
        if (budget == null) throw new IllegalArgumentException("対象予算が見つかりません。");
        TenantSession.requireScope(budget.getDepartmentId(), budget.getProjectId());
        if ("APPROVED".equals(decision)) {
            BigDecimal updated = request.getCurrentAmount().add(request.getChangeAmount());
            if (updated.signum() < 0) throw new IllegalStateException("変更後の予算が0円未満になります。");
            if (mapper.updateBudgetAmount(request.getBudgetId(), tenantId, updated, request.getCurrentAmount()) != 1)
                throw new IllegalStateException("対象予算が別の操作で変わりました。最新の金額で申請し直してください。");
        }
        if (mapper.review(id, tenantId, reviewerId, decision, comment) != 1) throw new IllegalStateException("申請はすでに審査済みか、自己承認はできません。");
        audit.record(decision, "BUDGET_CHANGE_REQUEST", id, "予算変更申請を" + decision);
    }
}
