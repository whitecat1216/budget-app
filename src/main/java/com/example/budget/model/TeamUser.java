package com.example.budget.model;

import lombok.Data;

@Data
public class TeamUser {
    private Long id;
    private Long tenantId;
    private String email;
    private String displayName;
    private String role;
    private String passwordHash;
    private boolean enabled;
}
