package com.example.budget.service;

import com.example.budget.mapper.AccountCodeMapper;
import com.example.budget.model.AccountCode;
import com.example.budget.security.TenantSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountCodeService {
    private final AccountCodeMapper mapper;
    private final AuditService auditService;

    public List<AccountCode> findAll() {
        return mapper.findAll(TenantSession.tenantId());
    }

    @Transactional
    public void create(AccountCode item) {
        long tenantId = TenantSession.tenantId();
        item.setTenantId(tenantId);
        if (item.getCode() == null || item.getCode().isBlank() || item.getName() == null || item.getName().isBlank()) {
            throw new IllegalArgumentException("科目コードと科目名を入力してください");
        }
        item.setCode(item.getCode().trim());
        item.setName(item.getName().trim());
        if (item.getCode().length() > 40 || item.getName().length() > 120) {
            throw new IllegalArgumentException("科目コードは40文字以内、科目名は120文字以内で入力してください");
        }
        if (!"INCOME".equals(item.getType()) && !"EXPENSE".equals(item.getType())) {
            throw new IllegalArgumentException("科目の種類を選択してください");
        }
        if (item.getCategoryId() != null && !mapper.categoryMatches(item.getCategoryId(), tenantId, item.getType())) {
            throw new IllegalArgumentException("同じ企業・種類のカテゴリを選択してください");
        }
        mapper.insert(item);
        auditService.record("CREATE", "ACCOUNT_CODE", item.getId(), "勘定科目を登録: " + item.getCode());
    }

    @Transactional
    public void toggleActive(Long id) {
        long tenantId = TenantSession.tenantId();
        String code = mapper.findCode(id, tenantId);
        if (code == null || mapper.toggleActive(id, tenantId) == 0) {
            throw new IllegalArgumentException("勘定科目が見つかりません");
        }
        auditService.record("UPDATE", "ACCOUNT_CODE", id, "勘定科目の有効状態を切替: " + code);
    }
}
