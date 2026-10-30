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
    private final com.example.budget.service.PasswordResetService passwordResetService;

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String verified,
                        @RequestParam(required = false) String verificationError,
                        @RequestParam(required = false) String sessionExpired,
                        @RequestParam(required = false) String mfaEnabled,
                        @RequestParam(required = false) String mfaDisabled, Model model) {
        if (error != null) model.addAttribute("errorMessage", "メールアドレスまたはパスワードを確認してください。");
        if (verified != null) model.addAttribute("successMessage", "メール認証が完了しました。ログインしてください。");
        if (verificationError != null) model.addAttribute("errorMessage", "認証リンクが無効か、有効期限が切れています。登録をやり直してください。");
        if (sessionExpired != null) model.addAttribute("errorMessage", "ユーザー権限が変更されたため、もう一度ログインしてください。");
        if (mfaEnabled != null) model.addAttribute("successMessage", "多要素認証を有効にしました。もう一度ログインしてください。");
        if (mfaDisabled != null) model.addAttribute("successMessage", "多要素認証を解除しました。もう一度ログインしてください。");
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

    @GetMapping("/forgot-password")
    public String forgotPassword(Model model) { return "forgot-password"; }

    @PostMapping("/forgot-password")
    public String requestPasswordReset(@RequestParam String email, RedirectAttributes redirect) {
        try { passwordResetService.request(email); } catch (RuntimeException ignored) { /* Keep response independent of account existence and mail state. */ }
        redirect.addFlashAttribute("successMessage", "該当するアカウントがあれば、再設定メールを送信しました。");
        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordForm(@RequestParam String token, Model model) { model.addAttribute("token", token); return "reset-password"; }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String token, @RequestParam String password,
                                @RequestParam String passwordConfirmation, RedirectAttributes redirect) {
        boolean changed = password.equals(passwordConfirmation) && passwordResetService.reset(token, password);
        if (changed) redirect.addFlashAttribute("successMessage", "パスワードを変更しました。ログインしてください。");
        else redirect.addFlashAttribute("errorMessage", "再設定リンクが無効か、入力内容を確認してください。");
        return "redirect:/login";
    }
}
