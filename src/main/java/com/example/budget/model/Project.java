package com.example.budget.model;

import lombok.Data;
import java.time.LocalDate;

@Data
public class Project {
    private Long id;
    private Long tenantId = 1L;
    private Long departmentId;
    private String departmentName;
    private String code;
    private String name;
    private String owner;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status = "ACTIVE";
}
