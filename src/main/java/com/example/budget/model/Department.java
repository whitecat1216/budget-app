package com.example.budget.model;

import lombok.Data;

@Data
public class Department {
    private Long id;
    private Long tenantId = 1L;
    private String code;
    private String name;
    private String manager;
    private String status = "ACTIVE";
}
