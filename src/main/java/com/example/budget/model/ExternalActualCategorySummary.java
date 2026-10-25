package com.example.budget.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExternalActualCategorySummary {
    private Long categoryId;
    private String categoryName;
    private Long departmentId;
    private Long projectId;
    private BigDecimal amount;
}
