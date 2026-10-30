package com.example.budget.controller;

import com.example.budget.model.ApprovalPolicy;
import com.example.budget.service.ApprovalPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/approval-policies")
@RequiredArgsConstructor
public class ApprovalPolicyController {
    private final ApprovalPolicyService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','FINANCE_ADMIN')")
    public String list(Model model) {
        model.addAttribute("activeNav", "approval-policies");
        model.addAttribute("policies", service.findAll());
        model.addAttribute("policy", new ApprovalPolicy());
        return "approval-policies";
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','FINANCE_ADMIN')")
    public String create(@ModelAttribute ApprovalPolicy policy, RedirectAttributes redirect) {
        try {
            service.create(policy);
            redirect.addFlashAttribute("message", "承認規程を追加しました");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/approval-policies";
    }

    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','FINANCE_ADMIN')")
    public String toggle(@PathVariable long id, RedirectAttributes redirect) {
        service.toggle(id);
        redirect.addFlashAttribute("message", "承認規程の状態を変更しました");
        return "redirect:/approval-policies";
    }
}
