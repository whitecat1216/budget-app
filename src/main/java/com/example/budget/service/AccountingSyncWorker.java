package com.example.budget.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountingSyncWorker {
    private final AccountingSyncService syncService;
    private final List<AccountingSyncProcessor> processors;

    @Scheduled(fixedDelayString = "${app.accounting-sync.poll-interval-ms:5000}")
    public void poll() {
        if (processors.isEmpty()) return;
        Map<String, AccountingSyncProcessor> byProvider = processors.stream()
                .collect(Collectors.toMap(p -> p.provider().toUpperCase(Locale.ROOT), Function.identity(), (first, ignored) -> first));
        var run = syncService.claimNext(byProvider.keySet().stream().toList());
        if (run == null) return;
        var processor = byProvider.get(run.getProvider().toUpperCase(Locale.ROOT));
        if (processor == null) {
            syncService.fail(run.getId(), run.getTenantId(), run.getAttemptCount(), run.getMaxAttempts(), false);
            return;
        }
        try {
            var context = new AccountingSyncContext(run.getId(), run.getTenantId(), run.getConnectionId(), run.getProvider(), run.getCursorValue());
            AccountingSyncProgress progress = (fetched, records, cursor) ->
                    syncService.recordPage(run.getId(), run.getTenantId(), run.getConnectionId(), fetched, records, cursor);
            syncService.complete(run.getId(), run.getTenantId(), processor.synchronize(context, progress));
        } catch (RetryableAccountingSyncException e) {
            log.warn("Accounting sync will retry: runId={}, correlationId={}, causeType={}", run.getId(), run.getCorrelationId(), e.getClass().getSimpleName());
            syncService.fail(run.getId(), run.getTenantId(), run.getAttemptCount(), run.getMaxAttempts(), true);
        } catch (RuntimeException e) {
            log.warn("Accounting sync failed: runId={}, correlationId={}, causeType={}", run.getId(), run.getCorrelationId(), e.getClass().getSimpleName());
            syncService.fail(run.getId(), run.getTenantId(), run.getAttemptCount(), run.getMaxAttempts(), false);
        }
    }
}
