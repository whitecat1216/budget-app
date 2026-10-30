package com.example.budget.controller;

import com.example.budget.model.AppUserPrincipal;
import com.example.budget.security.MfaGateFilter;
import com.example.budget.service.MfaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class MfaController {
    private final MfaService mfaService;

    @GetMapping("/mfa")
    public String settings(Model model, HttpSession session) {
        model.addAttribute("activeNav", "mfa");
        model.addAttribute("enabled", mfaService.isEnabled());
        model.addAttribute("encryptionConfigured", mfaService.encryptionConfigured());
        String pending = (String) session.getAttribute(MfaService.PENDING_SECRET);
        model.addAttribute("pendingSecret", pending);
        if (pending != null) model.addAttribute("provisioningUri", mfaService.provisioningUri(pending));
        return "mfa";
    }

    @PostMapping("/mfa/enroll")
    public String enroll(HttpSession session, RedirectAttributes redirect) {
        try {
            session.setAttribute(MfaService.PENDING_SECRET, mfaService.beginEnrollment());
            return "redirect:/mfa";
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/mfa";
        }
    }

    @PostMapping("/mfa/confirm")
    public String confirm(@RequestParam String code, HttpSession session, HttpServletRequest request,
                          RedirectAttributes redirect) {
        String secret = (String) session.getAttribute(MfaService.PENDING_SECRET);
        if (secret == null || !mfaService.confirmEnrollment(secret, code)) {
            redirect.addFlashAttribute("errorMessage", "認証コードを確認してください。");
            return "redirect:/mfa";
        }
        session.removeAttribute(MfaService.PENDING_SECRET);
        expireCurrentSession(request);
        return "redirect:/login?mfaEnabled=1";
    }

    @PostMapping("/mfa/disable")
    public String disable(@RequestParam String code, HttpServletRequest request, RedirectAttributes redirect) {
        if (!mfaService.disable(code)) {
            redirect.addFlashAttribute("errorMessage", "認証コードを確認してください。");
            return "redirect:/mfa";
        }
        expireCurrentSession(request);
        return "redirect:/login?mfaDisabled=1";
    }

    @GetMapping("/mfa/challenge")
    public String challenge(@RequestParam(required = false) String error, Model model, HttpSession session) {
        if (!Boolean.TRUE.equals(session.getAttribute(MfaGateFilter.MFA_REQUIRED))) return "redirect:/";
        if (error != null) model.addAttribute("errorMessage", "認証コードが正しくありません。");
        return "mfa-challenge";
    }

    @PostMapping("/mfa/challenge")
    public String verifyChallenge(@RequestParam String code, HttpSession session) {
        if (!Boolean.TRUE.equals(session.getAttribute(MfaGateFilter.MFA_REQUIRED))) return "redirect:/";
        if (!mfaService.verifyCurrentCode(code)) return "redirect:/mfa/challenge?error=1";
        session.removeAttribute(MfaGateFilter.MFA_REQUIRED);
        return "redirect:/";
    }

    private void expireCurrentSession(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
    }
}
