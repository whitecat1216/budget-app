package com.example.budget.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AccountCode {
    private Long id;
    private Long tenantId;
    private String code;
    private String name;
    private String type;
    private Long categoryId;
    private String categoryName;
    private boolean active = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
