package com.example.budget.service;

import com.example.budget.mapper.ApprovalPolicyMapper;
import com.example.budget.model.ApprovalPolicy;
import com.example.budget.security.TenantSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ApprovalPolicyService {
    private static final Set<String> ALLOWED_ROLES = Set.of("TENANT_ADMIN", "FINANCE_ADMIN", "APPROVER", "DEPARTMENT_MANAGER", "PROJECT_MANAGER");
    private final ApprovalPolicyMapper mapper;
    private final AuditService auditService;

    public List<ApprovalPolicy> findAll() { return mapper.findAll(TenantSession.tenantId()); }

    @Transactional
    public void create(ApprovalPolicy policy) {
        long tenantId = TenantSession.tenantId();
        if (policy.getPolicyName() == null || policy.getPolicyName().isBlank() || policy.getPolicyName().length() > 120)
            throw new IllegalArgumentException("規程名を入力してください（120文字以内）。");
        if (policy.getMinAmount() == null || policy.getMinAmount().signum() < 0)
            throw new IllegalArgumentException("下限金額を確認してください。");
        if (policy.getMaxAmount() != null && policy.getMaxAmount().compareTo(policy.getMinAmount()) <= 0)
            throw new IllegalArgumentException("上限金額は下限より大きくしてください。");
        List<String> roles = parseRoles(policy.getApprovalRoles());
        if (mapper.lockTenant(tenantId) == null) throw new IllegalStateException("企業が見つかりません。");
        if (mapper.countOverlaps(tenantId, policy.getMinAmount(), policy.getMaxAmount()) > 0)
            throw new IllegalArgumentException("有効な規程の金額範囲が重複しています。");
        policy.setTenantId(tenantId);
        policy.setApprovalRoles(String.join(",", roles));
        mapper.insert(policy);
        auditService.record("CREATE", "APPROVAL_POLICY", policy.getId(), "承認規程を登録: " + policy.getPolicyName());
    }

    @Transactional
    public void toggle(long id) {
        if (mapper.toggleEnabled(TenantSession.tenantId(), id) != 1)
            throw new IllegalArgumentException("承認規程が見つかりません。");
        auditService.record("UPDATE", "APPROVAL_POLICY", id, "承認規程の有効状態を切替");
    }

    public List<String> rolesForAmount(long tenantId, BigDecimal amount) {
        String route = mapper.findRolesForAmount(tenantId, amount);
        if (route == null) {
            if (mapper.countEnabled(tenantId) > 0)
                throw new IllegalStateException("申請金額に適用できる承認規程がありません。");
            return List.of("ANY_APPROVER");
        }
        return parseRoles(route);
    }

    private List<String> parseRoles(String raw) {
        if (raw == null || raw.isBlank()) throw new IllegalArgumentException("承認ロールを1つ以上指定してください。");
        List<String> roles = Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList();
        if (roles.isEmpty() || roles.size() > 8 || roles.stream().anyMatch(r -> !ALLOWED_ROLES.contains(r)))
            throw new IllegalArgumentException("承認経路は許可されたロールを順番に最大8段階で指定してください。");
        return roles;
    }
}
