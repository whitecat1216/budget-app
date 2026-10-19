package com.example.budget.controller;

import com.example.budget.model.Budget;
import com.example.budget.mapper.OrganizationMapper;
import com.example.budget.security.TenantSession;
import com.example.budget.service.BudgetService;
import com.example.budget.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.YearMonth;

@Controller
@RequestMapping("/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;
    private final CategoryService categoryService;
    private final OrganizationMapper organizationMapper;

    @GetMapping
    public String list(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            Model model) {

        YearMonth current = YearMonth.now();
        int y = year != null ? year : current.getYear();
        int m = month != null ? month : current.getMonthValue();

        model.addAttribute("budgets", budgetService.findByYearMonth(y, m));
        model.addAttribute("categories", categoryService.findByType("EXPENSE"));
        model.addAttribute("departments", organizationMapper.findDepartments(TenantSession.tenantId()).stream().filter(d -> TenantSession.canAccess(d.getId(), null)).toList());
        model.addAttribute("projects", organizationMapper.findProjects(TenantSession.tenantId()).stream().filter(p -> TenantSession.canAccess(p.getDepartmentId(), p.getId())).toList());
        model.addAttribute("budget", new Budget());
        model.addAttribute("year", y);
        model.addAttribute("month", m);
        model.addAttribute("activeNav", "budgets");
        model.addAttribute("prevYearMonth", YearMonth.of(y, m).minusMonths(1));
        model.addAttribute("nextYearMonth", YearMonth.of(y, m).plusMonths(1));
        return "budgets";
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','FINANCE_ADMIN')")
    public String create(@ModelAttribute Budget budget, RedirectAttributes ra) {
        budgetService.create(budget);
        ra.addFlashAttribute("message", "予算を設定しました");
        return "redirect:/budgets?year=" + budget.getYear() + "&month=" + budget.getMonth();
    }

    @PostMapping("/{id}/update")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','FINANCE_ADMIN')")
    public String update(@PathVariable Long id, @ModelAttribute Budget budget, RedirectAttributes ra) {
        budget.setId(id);
        budgetService.update(budget);
        ra.addFlashAttribute("message", "予算を更新しました");
        return "redirect:/budgets?year=" + budget.getYear() + "&month=" + budget.getMonth();
    }

}
