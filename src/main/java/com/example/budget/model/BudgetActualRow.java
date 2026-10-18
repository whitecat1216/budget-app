package com.example.budget.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BudgetActualRow {
    private Long categoryId;
    private String categoryName;
    private BigDecimal budget;
    private BigDecimal actual;

    public BigDecimal getRemaining() {
        return budget.subtract(actual);
    }

    public BigDecimal getExecutionRate() {
        if (budget.signum() == 0) {
            return null;
        }
        return actual.multiply(BigDecimal.valueOf(100))
                .divide(budget, 1, java.math.RoundingMode.HALF_UP);
    }

    public double getProgressWidth() {
        BigDecimal rate = getExecutionRate();
        if (rate == null || rate.signum() <= 0) {
            return 0;
        }
        return rate.min(BigDecimal.valueOf(100)).doubleValue();
    }

    public boolean isOverBudget() {
        BigDecimal rate = getExecutionRate();
        return rate != null && rate.compareTo(BigDecimal.valueOf(100)) >= 0;
    }
}
