package com.example.budget.service;

import com.example.budget.mapper.CategoryMapper;
import com.example.budget.model.Category;
import com.example.budget.security.TenantSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final AuditService auditService;

    public List<Category> findAll() {
        return categoryMapper.findAll(TenantSession.tenantId());
    }

    public Category findById(Long id) {
        return categoryMapper.findById(id, TenantSession.tenantId());
    }

    public List<Category> findByType(String type) {
        return categoryMapper.findByType(type, TenantSession.tenantId());
    }

    @Transactional
    public Category create(Category category) {
        category.setTenantId(TenantSession.tenantId());
        categoryMapper.insert(category);
        auditService.record("CREATE", "CATEGORY", category.getId(), "カテゴリを登録");
        return category;
    }

    @Transactional
    public Category update(Category category) {
        category.setTenantId(TenantSession.tenantId());
        categoryMapper.update(category);
        auditService.record("UPDATE", "CATEGORY", category.getId(), "カテゴリを更新");
        return categoryMapper.findById(category.getId(), TenantSession.tenantId());
    }

    @Transactional
    public void delete(Long id) {
        categoryMapper.delete(id, TenantSession.tenantId());
        auditService.record("DELETE", "CATEGORY", id, "カテゴリを削除");
    }
}
