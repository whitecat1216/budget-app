package com.example.budget.controller;

import com.example.budget.model.OnboardingForm;
import com.example.budget.service.OnboardingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class OnboardingController {
    private final OnboardingService onboardingService;

    @GetMapping("/onboarding")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public String show(Model model) {
        if (onboardingService.isComplete()) return "redirect:/";
        model.addAttribute("onboardingForm", onboardingService.getSettings());
        return "onboarding";
    }

    @PostMapping("/onboarding")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public String complete(@Valid @ModelAttribute("onboardingForm") OnboardingForm form,
                           BindingResult binding, Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) return "onboarding";
        try {
            onboardingService.complete(form);
            redirect.addFlashAttribute("message", "初期セットアップを保存しました。");
            return "redirect:/";
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "onboarding";
        }
    }
}
