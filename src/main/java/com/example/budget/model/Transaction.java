package com.example.budget.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Transaction {
    private Long id;
    private Long tenantId;
    private Long categoryId;
    private String categoryName; // joined
    private Long departmentId;
    private String departmentName;
    private Long projectId;
    private Long createdBy;
    private String projectName;
    private String type; // INCOME or EXPENSE
    private BigDecimal amount;
    private String description;
    private LocalDate transactionDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
