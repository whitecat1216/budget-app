package com.example.budget.controller;

import com.example.budget.mapper.CategoryMapper;
import com.example.budget.model.AccountCode;
import com.example.budget.security.TenantSession;
import com.example.budget.service.AccountCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/account-codes")
@RequiredArgsConstructor
public class AccountCodeController {
    private final AccountCodeService service;
    private final CategoryMapper categoryMapper;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("activeNav", "account-codes");
        model.addAttribute("accountCodes", service.findAll());
        model.addAttribute("categories", categoryMapper.findAll(TenantSession.tenantId()));
        model.addAttribute("accountCode", new AccountCode());
        return "account-codes";
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','FINANCE_ADMIN')")
    public String create(@ModelAttribute AccountCode accountCode, RedirectAttributes ra) {
        try {
            service.create(accountCode);
            ra.addFlashAttribute("message", "勘定科目を登録しました");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        } catch (org.springframework.dao.DuplicateKeyException e) {
            ra.addFlashAttribute("errorMessage", "同じ科目コードは登録できません");
        }
        return "redirect:/account-codes";
    }

    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','FINANCE_ADMIN')")
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        try {
            service.toggleActive(id);
            ra.addFlashAttribute("message", "勘定科目の状態を更新しました");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/account-codes";
    }
}
