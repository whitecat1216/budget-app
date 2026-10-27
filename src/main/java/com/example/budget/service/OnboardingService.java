package com.example.budget.service;

import com.example.budget.mapper.OnboardingMapper;
import com.example.budget.mapper.OrganizationMapper;
import com.example.budget.model.Department;
import com.example.budget.model.OnboardingForm;
import com.example.budget.security.TenantSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.ZoneId;
import java.util.Currency;

@Service
@RequiredArgsConstructor
public class OnboardingService {
    private final OnboardingMapper onboardingMapper;
    private final OrganizationMapper organizationMapper;
    private final AuditService auditService;

    public boolean isComplete() {
        return Boolean.TRUE.equals(onboardingMapper.isComplete(TenantSession.tenantId()));
    }

    public OnboardingForm getSettings() {
        OnboardingForm form = onboardingMapper.findSettings(TenantSession.tenantId());
        return form == null ? new OnboardingForm() : form;
    }

    @Transactional
    public void complete(OnboardingForm form) {
        long tenantId = TenantSession.tenantId();
        form.setCompanyName(form.getCompanyName().trim());
        form.setBaseCurrency(form.getBaseCurrency().trim().toUpperCase());
        form.setTimezone(form.getTimezone().trim());
        try {
            Currency.getInstance(form.getBaseCurrency());
            ZoneId.of(form.getTimezone());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("通貨またはタイムゾーンを確認してください。");
        }
        String code = form.getDepartmentCode() == null ? "" : form.getDepartmentCode().trim();
        String name = form.getDepartmentName() == null ? "" : form.getDepartmentName().trim();
        if (code.isEmpty() != name.isEmpty()) {
            throw new IllegalArgumentException("部門コードと部門名は両方入力してください。");
        }
        if (onboardingMapper.isComplete(tenantId)) {
            throw new IllegalStateException("初期セットアップはすでに完了しています。");
        }
        if (onboardingMapper.complete(tenantId, form) != 1) {
            throw new IllegalStateException("企業設定を保存できませんでした。");
        }
        if (!code.isEmpty()) {
            Department department = new Department();
            department.setTenantId(tenantId);
            department.setCode(code);
            department.setName(name);
            organizationMapper.insertDepartment(department);
        }
        auditService.record("COMPLETE", "ONBOARDING", tenantId, "初期セットアップを完了");
    }
}
