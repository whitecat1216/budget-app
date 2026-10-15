package com.example.budget.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Category {
    private Long id;
    private String name;
    private String type; // INCOME or EXPENSE
    private String color; // hex color for UI
    private String icon;  // optional icon name
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
