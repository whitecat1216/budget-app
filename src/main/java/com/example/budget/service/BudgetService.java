package com.example.budget.service;

import com.example.budget.mapper.BudgetMapper;
import com.example.budget.model.Budget;
import com.example.budget.security.TenantSession;
import com.example.budget.mapper.OrganizationMapper;
import com.example.budget.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetMapper budgetMapper;
    private final TransactionService transactionService;
    private final OrganizationMapper organizationMapper;
    private final CategoryService categoryService;
    private final AuditService auditService;

    public List<Budget> findAll() {
        return budgetMapper.findAll(TenantSession.tenantId());
    }

    public Budget findById(Long id) {
        Budget budget = budgetMapper.findById(id, TenantSession.tenantId());
        if (budget != null) {
            fillSpent(budget);
        }
        return budget;
    }

    public List<Budget> findByYearMonth(int year, int month) {
        List<Budget> budgets = budgetMapper.findByYearMonth(year, month, TenantSession.tenantId());
        budgets.forEach(this::fillSpent);
        return budgets;
    }

    public List<Budget> findByYearMonthAndScope(int year, int month, Long departmentId, Long projectId) {
        List<Budget> budgets = budgetMapper.findByYearMonthAndScope(year, month, departmentId, projectId, TenantSession.tenantId());
        budgets.forEach(this::fillSpent);
        return budgets;
    }

    public Budget findByCategoryAndYearMonth(Long categoryId, int year, int month) {
        Budget budget = budgetMapper.findByCategoryAndYearMonth(categoryId, year, month, TenantSession.tenantId());
        if (budget != null) {
            fillSpent(budget);
        }
        return budget;
    }

    @Transactional
    public Budget create(Budget budget) {
        validateDimensions(budget);
        budgetMapper.insert(budget);
        auditService.record("CREATE", "BUDGET", budget.getId(), budget.getYear() + "年" + budget.getMonth() + "月の予算を登録");
        return findById(budget.getId());
    }

    @Transactional
    public Budget update(Budget budget) {
        validateDimensions(budget);
        budgetMapper.update(budget);
        auditService.record("UPDATE", "BUDGET", budget.getId(), "予算を更新");
        return findById(budget.getId());
    }

    @Transactional
    public void delete(Long id) {
        budgetMapper.delete(id, TenantSession.tenantId());
        auditService.record("DELETE", "BUDGET", id, "予算を削除");
    }

    private void fillSpent(Budget budget) {
        BigDecimal spent = transactionService.getSpentByCategoryAndMonth(
                budget.getCategoryId(), budget.getYear(), budget.getMonth(),
                budget.getDepartmentId(), budget.getProjectId());
        budget.setSpent(spent != null ? spent : BigDecimal.ZERO);
    }

    private void validateDimensions(Budget budget) {
        long tenantId = TenantSession.tenantId();
        if (categoryService.findById(budget.getCategoryId()) == null) throw new IllegalArgumentException("カテゴリを確認してください。");
        if (budget.getDepartmentId() != null && !organizationMapper.departmentBelongsToTenant(budget.getDepartmentId(), tenantId))
            throw new IllegalArgumentException("部門を確認してください。");
        if (budget.getProjectId() != null && !organizationMapper.projectBelongsToTenant(budget.getProjectId(), tenantId))
            throw new IllegalArgumentException("プロジェクトを確認してください。");
        budget.setTenantId(tenantId);
    }
}
