package com.example.budget.controller;

import com.example.budget.model.Transaction;
import com.example.budget.service.CategoryService;
import com.example.budget.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final CategoryService categoryService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("transactions", transactionService.findAll());
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("transaction", new Transaction());
        model.addAttribute("today", LocalDate.now());
        return "transactions";
    }

    @PostMapping
    public String create(@ModelAttribute Transaction transaction, RedirectAttributes ra) {
        if (transaction.getTransactionDate() == null) {
            transaction.setTransactionDate(LocalDate.now());
        }
        transactionService.create(transaction);
        ra.addFlashAttribute("message", "取引を追加しました");
        return "redirect:/transactions";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id, @ModelAttribute Transaction transaction, RedirectAttributes ra) {
        transaction.setId(id);
        transactionService.update(transaction);
        ra.addFlashAttribute("message", "取引を更新しました");
        return "redirect:/transactions";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        transactionService.delete(id);
        ra.addFlashAttribute("message", "取引を削除しました");
        return "redirect:/transactions";
    }
}
