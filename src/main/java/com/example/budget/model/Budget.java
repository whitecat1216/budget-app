package com.example.budget.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Data
public class Budget {
    private Long id;
    private Long tenantId;
    private Long categoryId;
    private String categoryName; // joined
    private Long departmentId;
    private String departmentName;
    private Long projectId;
    private String projectName;
    private Integer year;
    private Integer month;
    private BigDecimal amount; // planned amount
    private BigDecimal spent;  // calculated from transactions
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public YearMonth getYearMonth() {
        return YearMonth.of(year, month);
    }
}
