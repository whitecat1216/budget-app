package com.example.budget.controller;

import com.example.budget.service.BudgetChangeRequestService;
import com.example.budget.service.BudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;

@Controller
@RequestMapping("/budget-change-requests")
@RequiredArgsConstructor
public class BudgetChangeRequestController {
    private final BudgetChangeRequestService requests;
    private final BudgetService budgets;

    @GetMapping
    public String list(@RequestParam(required = false) Long budgetId, Model model) {
        model.addAttribute("requests", requests.list());
        model.addAttribute("budgets", budgets.findAll());
        model.addAttribute("selectedBudgetId", budgetId);
        model.addAttribute("activeNav", "budget-changes");
        return "budget-change-requests";
    }

    @PostMapping
    public String create(@RequestParam long budgetId, @RequestParam BigDecimal changeAmount,
                         @RequestParam String reason, RedirectAttributes redirect) {
        try {
            requests.create(budgetId, changeAmount, reason);
            redirect.addFlashAttribute("message", "予算変更を申請しました。承認後に予算へ反映されます。");
        } catch (RuntimeException e) { redirect.addFlashAttribute("errorMessage", e.getMessage()); }
        return "redirect:/budget-change-requests";
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','FINANCE_ADMIN','APPROVER','DEPARTMENT_MANAGER','PROJECT_MANAGER')")
    public String review(@PathVariable long id, @RequestParam String decision,
                         @RequestParam(required = false) String comment, RedirectAttributes redirect) {
        try { requests.review(id, decision, comment); redirect.addFlashAttribute("message", "予算変更申請を処理しました。"); }
        catch (RuntimeException e) { redirect.addFlashAttribute("errorMessage", e.getMessage()); }
        return "redirect:/budget-change-requests";
    }
}
