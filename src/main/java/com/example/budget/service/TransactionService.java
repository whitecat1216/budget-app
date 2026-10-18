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
        return transactionMapper.findAll(TenantSession.tenantId());
    }

    public Transaction findById(Long id) {
        return transactionMapper.findById(id, TenantSession.tenantId());
    }

    public List<Transaction> findByDateRange(LocalDate start, LocalDate end) {
        return transactionMapper.findByDateRange(start, end, TenantSession.tenantId());
    }

    public List<Transaction> findByMonth(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        return transactionMapper.findByDateRange(ym.atDay(1), ym.atEndOfMonth(), TenantSession.tenantId());
    }

    public List<Transaction> findByCategory(Long categoryId) {
        return transactionMapper.findByCategory(categoryId, TenantSession.tenantId());
    }

    @Transactional
    public Transaction create(Transaction transaction) {
        validateDimensions(transaction);
        transactionMapper.insert(transaction);
        auditService.record("CREATE", "TRANSACTION", transaction.getId(), "取引を登録");
        return transactionMapper.findById(transaction.getId(), TenantSession.tenantId());
    }

    @Transactional
    public Transaction update(Transaction transaction) {
        validateDimensions(transaction);
        transactionMapper.update(transaction);
        auditService.record("UPDATE", "TRANSACTION", transaction.getId(), "取引を更新");
        return transactionMapper.findById(transaction.getId(), TenantSession.tenantId());
    }

    @Transactional
    public void delete(Long id) {
        transactionMapper.delete(id, TenantSession.tenantId());
        auditService.record("DELETE", "TRANSACTION", id, "取引を削除");
    }

    public BigDecimal getTotalIncome(LocalDate start, LocalDate end) {
        return transactionMapper.sumByTypeAndDateRange("INCOME", start, end, TenantSession.tenantId());
    }

    public BigDecimal getTotalExpense(LocalDate start, LocalDate end) {
        return transactionMapper.sumByTypeAndDateRange("EXPENSE", start, end, TenantSession.tenantId());
    }

    public BigDecimal getBalance(LocalDate start, LocalDate end) {
        return getTotalIncome(start, end).subtract(getTotalExpense(start, end));
    }

    public List<Map<String, Object>> getExpenseSummaryByCategory(LocalDate start, LocalDate end) {
        return transactionMapper.expenseSummaryByCategory(start, end, TenantSession.tenantId());
    }

    public List<CategoryExpenseSummary> getExpenseSummaryByCategoryIncludingEmpty(LocalDate start, LocalDate end) {
        return transactionMapper.expenseSummaryByCategoryIncludingEmpty(start, end, TenantSession.tenantId());
    }

    public List<CategoryExpenseSummary> getExpenseSummaryByCategoryAndScope(LocalDate start, LocalDate end,
                                                                            Long departmentId, Long projectId) {
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
        transaction.setTenantId(tenantId);
    }
}
