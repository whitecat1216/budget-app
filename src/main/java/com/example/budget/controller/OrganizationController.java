package com.example.budget.controller;

import com.example.budget.mapper.OrganizationMapper;
import com.example.budget.model.Department;
import com.example.budget.model.Project;
import com.example.budget.security.TenantSession;
import com.example.budget.service.AuditService;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/organization")
@RequiredArgsConstructor
public class OrganizationController {
    private final OrganizationMapper organizationMapper;
    private final AuditService auditService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("departments", organizationMapper.findDepartments(TenantSession.tenantId()));
        model.addAttribute("projects", organizationMapper.findProjects(TenantSession.tenantId()));
        model.addAttribute("department", new Department());
        model.addAttribute("project", new Project());
        model.addAttribute("activeNav", "organization");
        return "organization";
    }

    @PostMapping("/departments")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','FINANCE_ADMIN')")
    public String createDepartment(@ModelAttribute Department department, RedirectAttributes redirect) {
        department.setTenantId(TenantSession.tenantId());
        organizationMapper.insertDepartment(department);
        auditService.record("CREATE", "DEPARTMENT", department.getId(), "部門を登録");
        redirect.addFlashAttribute("message", "部門を登録しました");
        return "redirect:/organization";
    }

    @PostMapping("/projects")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','FINANCE_ADMIN','DEPARTMENT_MANAGER')")
    public String createProject(@ModelAttribute Project project, RedirectAttributes redirect) {
        project.setTenantId(TenantSession.tenantId());
        organizationMapper.insertProject(project);
        auditService.record("CREATE", "PROJECT", project.getId(), "プロジェクトを登録");
        redirect.addFlashAttribute("message", "プロジェクトを登録しました");
        return "redirect:/organization";
    }
}
