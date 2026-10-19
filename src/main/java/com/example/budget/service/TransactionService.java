package com.example.budget.service;

import com.example.budget.mapper.TransactionMapper;
import com.example.budget.model.Transaction;
import com.example.budget.model.CategoryExpenseSummary;
import com.example.budget.security.TenantSession;
import com.example.budget.mapper.OrganizationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionMapper transactionMapper;
    private final OrganizationMapper organizationMapper;
    private final CategoryService categoryService;
    private final AuditService auditService;

    public List<Transaction> findAll() {
        return transactionMapper.findAll(TenantSession.tenantId()).stream().filter(t -> TenantSession.canAccess(t.getDepartmentId(), t.getProjectId())).toList();
    }

    public Transaction findById(Long id) {
        Transaction transaction = transactionMapper.findById(id, TenantSession.tenantId());
        return transaction != null && TenantSession.canAccess(transaction.getDepartmentId(), transaction.getProjectId()) ? transaction : null;
    }

    public List<Transaction> findByDateRange(LocalDate start, LocalDate end) {
        return transactionMapper.findByDateRange(start, end, TenantSession.tenantId()).stream().filter(t -> TenantSession.canAccess(t.getDepartmentId(), t.getProjectId())).toList();
    }

    public List<Transaction> findByMonth(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        return transactionMapper.findByDateRange(ym.atDay(1), ym.atEndOfMonth(), TenantSession.tenantId());
    }

    public List<Transaction> findByCategory(Long categoryId) {
        return transactionMapper.findByCategory(categoryId, TenantSession.tenantId()).stream().filter(t -> TenantSession.canAccess(t.getDepartmentId(), t.getProjectId())).toList();
    }

    @Transactional
    public Transaction create(Transaction transaction) {
        validateDimensions(transaction);
        transaction.setCreatedBy(TenantSession.currentUser().getId());
        transactionMapper.insert(transaction);
        auditService.record("CREATE", "TRANSACTION", transaction.getId(), "取引を登録");
        return transactionMapper.findById(transaction.getId(), TenantSession.tenantId());
    }

    @Transactional
    public Transaction update(Transaction transaction) {
        Transaction existing = transactionMapper.findById(transaction.getId(), TenantSession.tenantId());
        if (existing == null) throw new IllegalArgumentException("取引が見つかりません。");
        TenantSession.requireScope(existing.getDepartmentId(), existing.getProjectId());
        requireTransactionEditPermission(existing);
        validateDimensions(transaction);
        transactionMapper.update(transaction);
        auditService.record("UPDATE", "TRANSACTION", transaction.getId(), "取引を更新");
        return transactionMapper.findById(transaction.getId(), TenantSession.tenantId());
    }

    @Transactional
    public void delete(Long id) {
        Transaction existing = transactionMapper.findById(id, TenantSession.tenantId());
        if (existing == null) throw new IllegalArgumentException("取引が見つかりません。");
        TenantSession.requireScope(existing.getDepartmentId(), existing.getProjectId());
        requireTransactionEditPermission(existing);
        transactionMapper.delete(id, TenantSession.tenantId());
        auditService.record("DELETE", "TRANSACTION", id, "取引を削除");
    }

    public BigDecimal getTotalIncome(LocalDate start, LocalDate end) {
        if (TenantSession.isScopeLimited()) return findByDateRange(start, end).stream().filter(t -> "INCOME".equals(t.getType())).map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return transactionMapper.sumByTypeAndDateRange("INCOME", start, end, TenantSession.tenantId());
    }

    public BigDecimal getTotalExpense(LocalDate start, LocalDate end) {
        if (TenantSession.isScopeLimited()) return findByDateRange(start, end).stream().filter(t -> "EXPENSE".equals(t.getType())).map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return transactionMapper.sumByTypeAndDateRange("EXPENSE", start, end, TenantSession.tenantId());
    }

    public BigDecimal getBalance(LocalDate start, LocalDate end) {
        return getTotalIncome(start, end).subtract(getTotalExpense(start, end));
    }

    public List<Map<String, Object>> getExpenseSummaryByCategory(LocalDate start, LocalDate end) {
        if (TenantSession.isScopeLimited()) return findByDateRange(start, end).stream().filter(t -> "EXPENSE".equals(t.getType()))
                .collect(java.util.stream.Collectors.groupingBy(t -> t.getCategoryName() == null ? "未分類" : t.getCategoryName(), java.util.stream.Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)))
                .entrySet().stream().map(e -> Map.<String,Object>of("category", e.getKey(), "total", e.getValue())).toList();
        return transactionMapper.expenseSummaryByCategory(start, end, TenantSession.tenantId());
    }

    public List<CategoryExpenseSummary> getExpenseSummaryByCategoryIncludingEmpty(LocalDate start, LocalDate end) {
        if (TenantSession.isScopeLimited()) {
            Map<Long, BigDecimal> actualByCategory = findByDateRange(start, end).stream().filter(t -> "EXPENSE".equals(t.getType()) && t.getCategoryId() != null)
                    .collect(java.util.stream.Collectors.groupingBy(Transaction::getCategoryId, java.util.stream.Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)));
            return categoryService.findByType("EXPENSE").stream().map(category -> {
                CategoryExpenseSummary summary = new CategoryExpenseSummary(); summary.setCategoryId(category.getId()); summary.setCategoryName(category.getName());
                summary.setActual(actualByCategory.getOrDefault(category.getId(), BigDecimal.ZERO)); return summary;
            }).toList();
        }
        return transactionMapper.expenseSummaryByCategoryIncludingEmpty(start, end, TenantSession.tenantId());
    }

    public List<CategoryExpenseSummary> getExpenseSummaryByCategoryAndScope(LocalDate start, LocalDate end,
                                                                            Long departmentId, Long projectId) {
        if (TenantSession.isScopeLimited()) return findByDateRange(start, end).stream().filter(t -> "EXPENSE".equals(t.getType()) && t.getCategoryId() != null)
                .filter(t -> departmentId == null || departmentId.equals(t.getDepartmentId()))
                .filter(t -> projectId == null || projectId.equals(t.getProjectId()))
                .collect(java.util.stream.Collectors.groupingBy(Transaction::getCategoryId,
                        java.util.stream.Collectors.groupingBy(t -> t.getCategoryName() == null ? "未分類" : t.getCategoryName(),
                                java.util.stream.Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add))))
                .entrySet().stream().map(entry -> { CategoryExpenseSummary summary = new CategoryExpenseSummary(); summary.setCategoryId(entry.getKey());
                    var nameAndAmount = entry.getValue().entrySet().stream().findFirst(); summary.setCategoryName(nameAndAmount.map(Map.Entry::getKey).orElse("未分類"));
                    summary.setActual(nameAndAmount.map(Map.Entry::getValue).orElse(BigDecimal.ZERO)); return summary; }).toList();
        return transactionMapper.expenseSummaryByCategoryAndScope(start, end, departmentId, projectId, TenantSession.tenantId());
    }

    public BigDecimal getSpentByCategoryAndMonth(Long categoryId, int year, int month,
                                                 Long departmentId, Long projectId) {
        return transactionMapper.sumExpenseByCategoryAndMonth(categoryId, year, month, departmentId, projectId, TenantSession.tenantId());
    }

    private void validateDimensions(Transaction transaction) {
        long tenantId = TenantSession.tenantId();
        if (transaction.getCategoryId() != null && categoryService.findById(transaction.getCategoryId()) == null)
            throw new IllegalArgumentException("カテゴリを確認してください。");
        if (transaction.getDepartmentId() != null && !organizationMapper.departmentBelongsToTenant(transaction.getDepartmentId(), tenantId))
            throw new IllegalArgumentException("部門を確認してください。");
        if (transaction.getProjectId() != null && !organizationMapper.projectBelongsToTenant(transaction.getProjectId(), tenantId))
            throw new IllegalArgumentException("プロジェクトを確認してください。");
        if (transaction.getProjectId() != null) {
            Long projectDepartmentId = organizationMapper.findProjectDepartmentId(transaction.getProjectId(), tenantId);
            if (projectDepartmentId != null && transaction.getDepartmentId() == null) transaction.setDepartmentId(projectDepartmentId);
            else if (projectDepartmentId != null && !projectDepartmentId.equals(transaction.getDepartmentId())) throw new IllegalArgumentException("プロジェクトに紐づく部門を選択してください。");
        }
        transaction.setTenantId(tenantId);
        TenantSession.requireScope(transaction.getDepartmentId(), transaction.getProjectId());
    }

    private void requireTransactionEditPermission(Transaction transaction) {
        var user = TenantSession.currentUser();
        if (user.getRole().equals("TENANT_ADMIN") || user.getRole().equals("FINANCE_ADMIN")) return;
        if (user.getRole().equals("DEPARTMENT_MANAGER") || user.getRole().equals("PROJECT_MANAGER")) return;
        if (transaction.getCreatedBy() != null && transaction.getCreatedBy().equals(user.getId())) return;
        throw new org.springframework.security.access.AccessDeniedException("この取引を変更する権限がありません。");
    }
}
