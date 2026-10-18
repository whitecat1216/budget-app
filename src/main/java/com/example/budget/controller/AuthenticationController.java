package com.example.budget.controller;

import com.example.budget.model.RegistrationForm;
import com.example.budget.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthenticationController {
    private final RegistrationService registrationService;

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String verified,
                        @RequestParam(required = false) String verificationError, Model model) {
        if (error != null) model.addAttribute("errorMessage", "メールアドレスまたはパスワードを確認してください。");
        if (verified != null) model.addAttribute("successMessage", "メール認証が完了しました。ログインしてください。");
        if (verificationError != null) model.addAttribute("errorMessage", "認証リンクが無効か、有効期限が切れています。登録をやり直してください。");
        return "login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("registrationForm", new RegistrationForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegistrationForm registrationForm,
                           BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) return "register";
        try {
            registrationService.register(registrationForm);
            return "registration-sent";
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "register";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "登録を完了できませんでした。メールアドレスが既に登録済みでないか確認してください。");
            return "register";
        }
    }

    @GetMapping("/verify-email")
    public String verify(@RequestParam String token, RedirectAttributes redirect) {
        if (registrationService.verify(token)) redirect.addAttribute("verified", "1");
        else redirect.addAttribute("verificationError", "1");
        return "redirect:/login";
    }
}
