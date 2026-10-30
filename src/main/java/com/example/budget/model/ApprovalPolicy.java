package com.example.budget.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ApprovalPolicy {
    private Long id;
    private Long tenantId;
    private String policyName;
    private BigDecimal minAmount = BigDecimal.ZERO;
    private BigDecimal maxAmount;
    private String approvalRoles;
    private boolean enabled = true;
    private LocalDateTime createdAt;
}
