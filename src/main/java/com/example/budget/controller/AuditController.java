package com.example.budget.controller;

import com.example.budget.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class AuditController {
    private final AuditService auditService;

    @GetMapping("/audit-logs")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','AUDITOR')")
    public String list(Model model) {
        model.addAttribute("events", auditService.recent());
        model.addAttribute("activeNav", "audit");
        return "audit-logs";
    }
}
