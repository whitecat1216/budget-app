package com.example.budget.service;

import com.example.budget.mapper.CategoryMapper;
import com.example.budget.model.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;

    public List<Category> findAll() {
        return categoryMapper.findAll();
    }

    public Category findById(Long id) {
        return categoryMapper.findById(id);
    }

    public List<Category> findByType(String type) {
        return categoryMapper.findByType(type);
    }

    @Transactional
    public Category create(Category category) {
        categoryMapper.insert(category);
        return category;
    }

    @Transactional
    public Category update(Category category) {
        categoryMapper.update(category);
        return categoryMapper.findById(category.getId());
    }

    @Transactional
    public void delete(Long id) {
        categoryMapper.delete(id);
    }
}
