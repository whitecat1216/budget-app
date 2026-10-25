package com.example.budget.model;

import java.time.LocalDateTime;

public record AccountingSyncRunResponse(
        Long id,
        String provider,
        String status,
        int fetchedCount,
        int importedCount,
        int attemptCount,
        String errorMessage,
        String correlationId,
        LocalDateTime queuedAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        LocalDateTime nextAttemptAt) {
    public static AccountingSyncRunResponse from(AccountingSyncRun run) {
        return new AccountingSyncRunResponse(run.getId(), run.getProvider(), run.getStatus(), run.getFetchedCount(),
                run.getImportedCount(), run.getAttemptCount(), run.getErrorMessage(), run.getCorrelationId(),
                run.getQueuedAt(), run.getStartedAt(), run.getFinishedAt(), run.getNextAttemptAt());
    }
}
