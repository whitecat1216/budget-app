package com.example.budget.controller;

import com.example.budget.service.AccountingSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class AccountingSyncPageController {
    private final AccountingSyncService syncService;

    @GetMapping("/accounting")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','FINANCE_ADMIN','AUDITOR')")
    public String index(Model model) {
        model.addAttribute("connections", syncService.connections());
        model.addAttribute("syncRuns", syncService.recentRuns());
        model.addAttribute("supportedProviders", syncService.supportedProviders());
        model.addAttribute("activeNav", "accounting");
        return "accounting";
    }
}
