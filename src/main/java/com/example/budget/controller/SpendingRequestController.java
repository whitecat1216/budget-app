package com.example.budget.controller;

import com.example.budget.mapper.OrganizationMapper;
import com.example.budget.model.SpendingRequest;
import com.example.budget.security.TenantSession;
import com.example.budget.service.CategoryService;
import com.example.budget.service.SpendingRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;

@Controller
@RequestMapping("/requests")
@RequiredArgsConstructor
public class SpendingRequestController {
    private final SpendingRequestService requestService;
    private final OrganizationMapper organizationMapper;
    private final CategoryService categoryService;

    @GetMapping
    public String list(Model model) {
        var user = TenantSession.currentUser();
        boolean canReview = user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_TENANT_ADMIN") || a.getAuthority().equals("ROLE_FINANCE_ADMIN") || a.getAuthority().equals("ROLE_APPROVER") || a.getAuthority().equals("ROLE_DEPARTMENT_MANAGER") || a.getAuthority().equals("ROLE_PROJECT_MANAGER"));
        model.addAttribute("requests", requestService.list());
        model.addAttribute("request", new SpendingRequest());
        model.addAttribute("departments", organizationMapper.findDepartments(user.getTenantId()));
        model.addAttribute("projects", organizationMapper.findProjects(user.getTenantId()));
        model.addAttribute("categories", categoryService.findByType("EXPENSE"));
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("canReview", canReview);
        model.addAttribute("activeNav", "requests");
        return "requests";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("request") SpendingRequest request, BindingResult binding, Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            list(model);
            model.addAttribute("request", request);
            model.addAttribute("formErrors", true);
            return "requests";
        }
        requestService.create(request);
        redirect.addFlashAttribute("message", "申請を下書き保存しました");
        return "redirect:/requests";
    }

    @PostMapping("/{id}/submit")
    public String submit(@PathVariable long id, RedirectAttributes redirect) {
        requestService.submit(id);
        redirect.addFlashAttribute("message", "申請を提出しました");
        return "redirect:/requests";
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','FINANCE_ADMIN','APPROVER','DEPARTMENT_MANAGER','PROJECT_MANAGER')")
    public String review(@PathVariable long id, @RequestParam String action,
                         @RequestParam(required = false) String comment, RedirectAttributes redirect) {
        requestService.review(id, action, comment);
        redirect.addFlashAttribute("message", "申請を処理しました");
        return "redirect:/requests";
    }
}
