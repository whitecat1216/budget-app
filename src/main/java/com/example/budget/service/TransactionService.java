package com.example.budget.service;

import com.example.budget.mapper.TransactionMapper;
import com.example.budget.model.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionMapper transactionMapper;

    public List<Transaction> findAll() {
        return transactionMapper.findAll();
    }

    public Transaction findById(Long id) {
        return transactionMapper.findById(id);
    }

    public List<Transaction> findByDateRange(LocalDate start, LocalDate end) {
        return transactionMapper.findByDateRange(start, end);
    }

    public List<Transaction> findByMonth(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        return transactionMapper.findByDateRange(ym.atDay(1), ym.atEndOfMonth());
    }

    public List<Transaction> findByCategory(Long categoryId) {
        return transactionMapper.findByCategory(categoryId);
    }

    @Transactional
    public Transaction create(Transaction transaction) {
        transactionMapper.insert(transaction);
        return transactionMapper.findById(transaction.getId());
    }

    @Transactional
    public Transaction update(Transaction transaction) {
        transactionMapper.update(transaction);
        return transactionMapper.findById(transaction.getId());
    }

    @Transactional
    public void delete(Long id) {
        transactionMapper.delete(id);
    }

    public BigDecimal getTotalIncome(LocalDate start, LocalDate end) {
        return transactionMapper.sumByTypeAndDateRange("INCOME", start, end);
    }

    public BigDecimal getTotalExpense(LocalDate start, LocalDate end) {
        return transactionMapper.sumByTypeAndDateRange("EXPENSE", start, end);
    }

    public BigDecimal getBalance(LocalDate start, LocalDate end) {
        return getTotalIncome(start, end).subtract(getTotalExpense(start, end));
    }

    public List<Map<String, Object>> getExpenseSummaryByCategory(LocalDate start, LocalDate end) {
        return transactionMapper.expenseSummaryByCategory(start, end);
    }

    public BigDecimal getSpentByCategoryAndMonth(Long categoryId, int year, int month) {
        return transactionMapper.sumExpenseByCategoryAndMonth(categoryId, year, month);
    }
}
