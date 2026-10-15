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
import java.util.List;
import java.util.Map;

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

        List<Map<String, Object>> expenseByCategory = transactionService.getExpenseSummaryByCategory(start, end);
        var budgets = budgetService.findByYearMonth(y, m);
        var recentTransactions = transactionService.findByMonth(y, m);

        model.addAttribute("year", y);
        model.addAttribute("month", m);
        model.addAttribute("income", income);
        model.addAttribute("expense", expense);
        model.addAttribute("balance", balance);
        model.addAttribute("expenseByCategory", expenseByCategory);
        model.addAttribute("budgets", budgets);
        model.addAttribute("transactions", recentTransactions);
        model.addAttribute("prevYearMonth", ym.minusMonths(1));
        model.addAttribute("nextYearMonth", ym.plusMonths(1));

        return "dashboard";
    }
}
