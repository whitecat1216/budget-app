package com.example.budget.controller;

import com.example.budget.service.BudgetService;
import com.example.budget.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final TransactionService transactionService;
    private final BudgetService budgetService;

    @GetMapping("/")
    public String dashboard(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            Model model) {

        YearMonth current = YearMonth.now();
        int y = year != null ? year : current.getYear();
        int m = month != null ? month : current.getMonthValue();

        YearMonth ym = YearMonth.of(y, m);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        BigDecimal income = transactionService.getTotalIncome(start, end);
        BigDecimal expense = transactionService.getTotalExpense(start, end);
        BigDecimal balance = income.subtract(expense);

        var budgets = budgetService.findByYearMonth(y, m);
        var recentTransactions = transactionService.findByMonth(y, m);
        BigDecimal totalBudget = budgets.stream()
                .map(budget -> budget.getAmount() != null ? budget.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal budgetUsageRate = totalBudget.signum() == 0
                ? BigDecimal.ZERO
                : expense.multiply(BigDecimal.valueOf(100))
                    .divide(totalBudget, 1, java.math.RoundingMode.HALF_UP);
        double budgetProgressWidth = budgetUsageRate.max(BigDecimal.ZERO)
                .min(BigDecimal.valueOf(100)).doubleValue();

        var categoryExpenses = transactionService.getExpenseSummaryByCategoryIncludingEmpty(start, end);
        BigDecimal maxCategoryExpense = categoryExpenses.stream()
                .map(row -> row.getActual() != null ? row.getActual() : BigDecimal.ZERO)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        categoryExpenses.forEach(row -> {
            BigDecimal amount = row.getActual() != null ? row.getActual() : BigDecimal.ZERO;
            row.setProgressWidth(maxCategoryExpense.signum() == 0
                    ? 0
                    : amount.multiply(BigDecimal.valueOf(100))
                        .divide(maxCategoryExpense, 1, java.math.RoundingMode.HALF_UP).doubleValue());
        });

        model.addAttribute("year", y);
        model.addAttribute("month", m);
        model.addAttribute("activeNav", "dashboard");
        model.addAttribute("income", income);
        model.addAttribute("expense", expense);
        model.addAttribute("balance", balance);
        model.addAttribute("categoryExpenses", categoryExpenses);
        model.addAttribute("totalBudget", totalBudget);
        model.addAttribute("budgetUsageRate", budgetUsageRate);
        model.addAttribute("budgetProgressWidth", budgetProgressWidth);
        model.addAttribute("budgetExceeded", budgetUsageRate.compareTo(BigDecimal.valueOf(100)) >= 0);
        model.addAttribute("budgets", budgets);
        model.addAttribute("transactions", recentTransactions);
        model.addAttribute("prevYearMonth", ym.minusMonths(1));
        model.addAttribute("nextYearMonth", ym.plusMonths(1));

        return "dashboard";
    }
}
