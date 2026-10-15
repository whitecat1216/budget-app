package com.example.budget.controller;

import com.example.budget.model.Category;
import com.example.budget.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("category", new Category());
        return "categories";
    }

    @PostMapping
    public String create(@ModelAttribute Category category, RedirectAttributes ra) {
        if (category.getColor() == null || category.getColor().isBlank()) {
            category.setColor("#6c757d");
        }
        categoryService.create(category);
        ra.addFlashAttribute("message", "カテゴリを追加しました");
        return "redirect:/categories";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id, @ModelAttribute Category category, RedirectAttributes ra) {
        category.setId(id);
        categoryService.update(category);
        ra.addFlashAttribute("message", "カテゴリを更新しました");
        return "redirect:/categories";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        categoryService.delete(id);
        ra.addFlashAttribute("message", "カテゴリを削除しました");
        return "redirect:/categories";
    }
}
