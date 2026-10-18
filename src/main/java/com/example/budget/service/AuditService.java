package com.example.budget.service;

import com.example.budget.mapper.AuditMapper;
import com.example.budget.security.TenantSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditMapper auditMapper;

    @Transactional
    public void record(String action, String type, Object id, String details) {
        var user = TenantSession.currentUser();
        auditMapper.insert(user.getTenantId(), user.getId(), action, type,
                id == null ? null : id.toString(), details == null ? null : details.substring(0, Math.min(details.length(), 1000)));
    }

    @Transactional
    public void recordFor(long tenantId, Long actorId, String action, String type, String id, String details) {
        auditMapper.insert(tenantId, actorId, action, type, id, details == null ? null : details.substring(0, Math.min(details.length(), 1000)));
    }

    public List<Map<String, Object>> recent() { return auditMapper.findRecent(TenantSession.tenantId()); }
}
