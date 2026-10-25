package com.example.budget.model;

import lombok.Data;

@Data
public class AccountingConnectionInfo {
    private Long id;
    private Long tenantId;
    private String provider;
    private String displayName;
    private String status;
}
