package com.example.budget.service;

import com.example.budget.mapper.OrganizationMapper;
import com.example.budget.mapper.TransactionMapper;
import com.example.budget.model.AppUserPrincipal;
import com.example.budget.model.Transaction;
import com.example.budget.security.TenantSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceScopeTest {
    @Mock TransactionMapper transactions;
    @Mock OrganizationMapper organizations;
    @Mock CategoryService categories;
    @Mock AuditService audit;
    private TransactionService service;

    @BeforeEach
    void setUp() {
        service = new TransactionService(transactions, organizations, categories, audit);
        AppUserPrincipal principal = new AppUserPrincipal(7L, 3L, "manager@example.com", "責任者", "hash",
                "DEPARTMENT_MANAGER", 0);
        principal.setScopes(Set.of(11L), Set.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void monthListFiltersTransactionsOutsideAssignedDepartment() {
        when(transactions.findByDateRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), 3L))
                .thenReturn(List.of(transaction(11L, 101L, "EXPENSE", "100"),
                        transaction(12L, 102L, "EXPENSE", "900")));

        List<Transaction> result = service.findByMonth(2026, 10);

        assertEquals(1, result.size());
        assertEquals(101L, result.get(0).getId());
    }

    @Test
    void categoryMonthSpendOnlyIncludesAssignedDepartment() {
        when(transactions.findByDateRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), 3L))
                .thenReturn(List.of(transaction(11L, 101L, "EXPENSE", "100"),
                        transaction(12L, 102L, "EXPENSE", "900")));

        BigDecimal spent = service.getSpentByCategoryAndMonth(5L, 2026, 10, 11L, null);

        assertEquals(new BigDecimal("100"), spent);
    }

    private static Transaction transaction(Long departmentId, Long id, String type, String amount) {
        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setTenantId(3L);
        transaction.setDepartmentId(departmentId);
        transaction.setCategoryId(5L);
        transaction.setType(type);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setTransactionDate(LocalDate.of(2026, 10, 15));
        return transaction;
    }
}
