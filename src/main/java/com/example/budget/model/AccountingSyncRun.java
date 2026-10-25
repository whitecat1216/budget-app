package com.example.budget.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AccountingSyncRun {
    private Long id;
    private Long tenantId;
    private Long connectionId;
    private Long requestedBy;
    private String provider;
    private String correlationId;
    private String status;
    private int fetchedCount;
    private int importedCount;
    private int attemptCount;
    private int maxAttempts;
    private String cursorValue;
    private String errorMessage;
    private LocalDateTime queuedAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime nextAttemptAt;
}
