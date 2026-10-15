package com.example.budget.service;

import com.example.budget.mapper.BudgetMapper;
import com.example.budget.model.Budget;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetMapper budgetMapper;
    private final TransactionService transactionService;

    public List<Budget> findAll() {
        return budgetMapper.findAll();
    }

    public Budget findById(Long id) {
        Budget budget = budgetMapper.findById(id);
        if (budget != null) {
            fillSpent(budget);
        }
        return budget;
    }

    public List<Budget> findByYearMonth(int year, int month) {
        List<Budget> budgets = budgetMapper.findByYearMonth(year, month);
        budgets.forEach(this::fillSpent);
        return budgets;
    }

    public Budget findByCategoryAndYearMonth(Long categoryId, int year, int month) {
        Budget budget = budgetMapper.findByCategoryAndYearMonth(categoryId, year, month);
        if (budget != null) {
            fillSpent(budget);
        }
        return budget;
    }

    @Transactional
    public Budget create(Budget budget) {
        budgetMapper.insert(budget);
        return findById(budget.getId());
    }

    @Transactional
    public Budget update(Budget budget) {
        budgetMapper.update(budget);
        return findById(budget.getId());
    }

    @Transactional
    public void delete(Long id) {
        budgetMapper.delete(id);
    }

    private void fillSpent(Budget budget) {
        BigDecimal spent = transactionService.getSpentByCategoryAndMonth(
                budget.getCategoryId(), budget.getYear(), budget.getMonth());
        budget.setSpent(spent != null ? spent : BigDecimal.ZERO);
    }
}
