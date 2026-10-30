package com.example.budget.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.validation.constraints.*;

@Data
public class SpendingRequest {
    private Long id;
    private Long tenantId;
    private Long requesterId;
    private String requesterName;
    private Long departmentId;
    private String departmentName;
    private Long projectId;
    private String projectName;
    private Long categoryId;
    private String categoryName;
    @NotBlank @Size(max = 160) private String title;
    @NotBlank @Size(max = 1000) private String purpose;
    @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) private BigDecimal amount;
    private LocalDate requestedDate;
    private String status;
    private int approvalRound;
    private int currentApprovalStep;
    private int approvalStepCount;
    private String currentApprovalRole;
    private boolean canReview;
    private LocalDateTime createdAt;
}
