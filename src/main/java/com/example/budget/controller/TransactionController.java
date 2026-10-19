package com.example.budget.controller;

import com.example.budget.model.Transaction;
import com.example.budget.mapper.OrganizationMapper;
import com.example.budget.security.TenantSession;
import com.example.budget.service.CategoryService;
import com.example.budget.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDate;

@Controller
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final CategoryService categoryService;
    private final OrganizationMapper organizationMapper;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("activeNav", "transactions");
        model.addAttribute("transactions", transactionService.findAll());
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("departments", organizationMapper.findDepartments(TenantSession.tenantId()).stream().filter(d -> TenantSession.canAccess(d.getId(), null)).toList());
        model.addAttribute("projects", organizationMapper.findProjects(TenantSession.tenantId()).stream().filter(p -> TenantSession.canAccess(p.getDepartmentId(), p.getId())).toList());
        model.addAttribute("transaction", new Transaction());
        model.addAttribute("today", LocalDate.now());
        return "transactions";
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','FINANCE_ADMIN','MEMBER','DEPARTMENT_MANAGER','PROJECT_MANAGER')")
    public String create(@ModelAttribute Transaction transaction, RedirectAttributes ra) {
        if (transaction.getTransactionDate() == null) {
            transaction.setTransactionDate(LocalDate.now());
        }
        transactionService.create(transaction);
        ra.addFlashAttribute("message", "取引を追加しました");
        return "redirect:/transactions";
    }

    @PostMapping("/{id}/update")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','FINANCE_ADMIN','MEMBER','DEPARTMENT_MANAGER','PROJECT_MANAGER')")
    public String update(@PathVariable Long id, @ModelAttribute Transaction transaction, RedirectAttributes ra) {
        transaction.setId(id);
        transactionService.update(transaction);
        ra.addFlashAttribute("message", "取引を更新しました");
        return "redirect:/transactions";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','FINANCE_ADMIN','MEMBER','DEPARTMENT_MANAGER','PROJECT_MANAGER')")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        transactionService.delete(id);
        ra.addFlashAttribute("message", "取引を削除しました");
        return "redirect:/transactions";
    }
}
