package com.example.budget.controller;

import com.example.budget.model.InvitationForm;
import com.example.budget.service.TeamService;
import com.example.budget.mapper.OrganizationMapper;
import com.example.budget.security.TenantSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class TeamController {
    private final TeamService teamService;
    private final OrganizationMapper organizationMapper;

    @GetMapping("/team")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public String list(Model model) {
        model.addAttribute("users", teamService.list());
        model.addAttribute("invite", new InvitationForm());
        model.addAttribute("departments", organizationMapper.findDepartments(TenantSession.tenantId()));
        model.addAttribute("projects", organizationMapper.findProjects(TenantSession.tenantId()));
        model.addAttribute("activeNav", "team");
        return "team";
    }

    @PostMapping("/team/invite")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public String invite(@Valid @ModelAttribute("invite") InvitationForm invite, BindingResult binding,
                         Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("users", teamService.list()); model.addAttribute("departments", organizationMapper.findDepartments(TenantSession.tenantId())); model.addAttribute("projects", organizationMapper.findProjects(TenantSession.tenantId())); model.addAttribute("activeNav", "team");
            return "team";
        }
        try {
            teamService.invite(invite);
            redirect.addFlashAttribute("message", "招待メールを送信しました");
            return "redirect:/team";
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage()); model.addAttribute("users", teamService.list()); model.addAttribute("departments", organizationMapper.findDepartments(TenantSession.tenantId())); model.addAttribute("projects", organizationMapper.findProjects(TenantSession.tenantId()));
            model.addAttribute("activeNav", "team"); return "team";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "招待を送信できませんでした。メールアドレスが既に登録されていないか確認してください。");
            model.addAttribute("users", teamService.list()); model.addAttribute("departments", organizationMapper.findDepartments(TenantSession.tenantId())); model.addAttribute("projects", organizationMapper.findProjects(TenantSession.tenantId())); model.addAttribute("activeNav", "team"); return "team";
        }
    }

    @PostMapping("/team/{userId}/update")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public String updateUser(@PathVariable long userId, @RequestParam String role,
                             @RequestParam(defaultValue = "false") boolean enabled,
                             @RequestParam(required = false) java.util.List<Long> departmentIds,
                             @RequestParam(required = false) java.util.List<Long> projectIds,
                             RedirectAttributes redirect) {
        try {
            teamService.updateUser(userId, role, enabled, departmentIds, projectIds);
            redirect.addFlashAttribute("message", "ユーザー権限を更新しました。");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/team";
    }

    @GetMapping("/accept-invitation")
    public String acceptForm(@RequestParam String token, Model model) {
        model.addAttribute("token", token); return "accept-invitation";
    }

    @PostMapping("/accept-invitation")
    public String accept(@RequestParam String token, @RequestParam String password,
                         @RequestParam String passwordConfirmation, RedirectAttributes redirect) {
        if (!password.equals(passwordConfirmation) || !teamService.accept(token, password)) {
            redirect.addAttribute("inviteError", "1"); return "redirect:/accept-invitation?token=" + token;
        }
        redirect.addAttribute("verified", "1"); return "redirect:/login";
    }
}
