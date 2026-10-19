package com.example.budget.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BudgetChangeRequest {
    private Long id;
    private Long tenantId;
    private Long budgetId;
    private Long departmentId;
    private Long projectId;
    private Long requesterId;
    private Long reviewerId;
    private BigDecimal changeAmount;
    private BigDecimal currentAmount;
    private String reason;
    private String status;
    private String requesterName;
    private String categoryName;
    private Integer year;
    private Integer month;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
    private String reviewComment;
}
