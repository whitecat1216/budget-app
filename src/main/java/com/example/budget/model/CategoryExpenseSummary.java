package com.example.budget.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CategoryExpenseSummary {
    private Long categoryId;
    private String categoryName;
    private BigDecimal actual;
    private double progressWidth;
}
