package com.example.budget.controller;

import com.example.budget.model.BudgetActualRow;
import com.example.budget.service.BudgetService;
import com.example.budget.service.TransactionService;
import com.example.budget.mapper.OrganizationMapper;
import com.example.budget.security.TenantSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final BudgetService budgetService;
    private final TransactionService transactionService;
    private final OrganizationMapper organizationMapper;

    @GetMapping("/budget-vs-actual")
    public String budgetVsActual(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long projectId,
            Model model) {
        YearMonth selectedMonth = YearMonth.of(
                year != null ? year : YearMonth.now().getYear(),
                month != null ? month : YearMonth.now().getMonthValue());

        Map<Long, BigDecimal> budgetsByCategory = new HashMap<>();
        budgetService.findByYearMonth(selectedMonth.getYear(), selectedMonth.getMonthValue())
                .forEach(budget -> budgetsByCategory.merge(budget.getCategoryId(), budget.getAmount(), BigDecimal::add));

        if (departmentId != null || projectId != null) {
            budgetsByCategory.clear();
            budgetService.findByYearMonthAndScope(selectedMonth.getYear(), selectedMonth.getMonthValue(), departmentId, projectId)
                    .forEach(budget -> budgetsByCategory.merge(budget.getCategoryId(), budget.getAmount(), BigDecimal::add));
        }

        List<BudgetActualRow> rows = transactionService.getExpenseSummaryByCategoryAndScope(
                        selectedMonth.atDay(1), selectedMonth.atEndOfMonth(), departmentId, projectId)
                .stream()
                .map(summary -> {
                    BudgetActualRow row = new BudgetActualRow();
                    row.setCategoryId(summary.getCategoryId());
                    row.setCategoryName(summary.getCategoryName());
                    row.setBudget(budgetsByCategory.getOrDefault(summary.getCategoryId(), BigDecimal.ZERO));
                    row.setActual(summary.getActual());
                    return row;
                })
                .toList();

        BigDecimal totalBudget = rows.stream()
                .map(BudgetActualRow::getBudget)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalActual = rows.stream()
                .map(BudgetActualRow::getActual)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("rows", rows);
        model.addAttribute("totalBudget", totalBudget);
        model.addAttribute("totalActual", totalActual);
        model.addAttribute("totalRemaining", totalBudget.subtract(totalActual));
        model.addAttribute("activeNav", "reports");
        model.addAttribute("departments", organizationMapper.findDepartments(TenantSession.tenantId()).stream().filter(d -> TenantSession.canAccess(d.getId(), null)).toList());
        model.addAttribute("projects", organizationMapper.findProjects(TenantSession.tenantId()).stream().filter(p -> TenantSession.canAccess(p.getDepartmentId(), p.getId())).toList());
        model.addAttribute("departmentId", departmentId);
        model.addAttribute("projectId", projectId);
        model.addAttribute("selectedMonth", selectedMonth);
        model.addAttribute("year", selectedMonth.getYear());
        model.addAttribute("month", selectedMonth.getMonthValue());
        model.addAttribute("prevYearMonth", selectedMonth.minusMonths(1));
        model.addAttribute("nextYearMonth", selectedMonth.plusMonths(1));
        return "report";
    }
}
