package com.example.budget.service;

import com.example.budget.mapper.AccountingSyncMapper;
import com.example.budget.model.AccountingConnectionInfo;
import com.example.budget.model.AccountingSyncRun;
import com.example.budget.model.ExternalActualRecord;
import com.example.budget.security.TenantSession;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountingSyncService {
    private final AccountingSyncMapper mapper;
    private final List<AccountingSyncProcessor> processors;
    private final AuditService auditService;

    @Transactional
    public AccountingSyncRun requestSync(long connectionId) {
        var user = TenantSession.currentUser();
        var connection = mapper.findConnection(user.getTenantId(), connectionId);
        if (connection == null) throw new IllegalArgumentException("会計接続が見つかりません。");
        if (!"CONNECTED".equals(connection.getStatus())) throw new IllegalStateException("会計接続が未設定または未接続です。");
        if (processors.stream().noneMatch(p -> p.provider().equalsIgnoreCase(connection.getProvider()))) {
            throw new AccountingProviderUnavailableException(connection.getProvider());
        }

        var run = new AccountingSyncRun();
        run.setTenantId(user.getTenantId());
        run.setConnectionId(connectionId);
        run.setRequestedBy(user.getId());
        run.setCorrelationId(UUID.randomUUID().toString());
        run.setProvider(connection.getProvider());
        run.setStatus("QUEUED");
        run.setQueuedAt(LocalDateTime.now());
        try {
            if (mapper.enqueue(run) != 1) throw new IllegalStateException("会計接続の状態を確認できません。");
        } catch (DataIntegrityViolationException e) {
            throw new AccountingSyncAlreadyRunningException();
        }
        auditService.record("REQUEST_SYNC", "ACCOUNTING_SYNC_RUN", run.getId(), "会計同期を受付");
        return run;
    }

    public List<AccountingConnectionInfo> connections() {
        return mapper.findConnections(TenantSession.tenantId());
    }

    public List<String> supportedProviders() {
        return processors.stream().map(AccountingSyncProcessor::provider).distinct().sorted().toList();
    }

    public List<AccountingSyncRun> recentRuns() {
        return mapper.findRecent(TenantSession.tenantId());
    }

    @Transactional
    public AccountingSyncRun retry(long runId) {
        var user = TenantSession.currentUser();
        var oldRun = mapper.findRun(user.getTenantId(), runId);
        if (oldRun == null) throw new IllegalArgumentException("同期履歴が見つかりません。");
        if (processors.stream().noneMatch(p -> p.provider().equalsIgnoreCase(oldRun.getProvider()))) {
            throw new AccountingProviderUnavailableException(oldRun.getProvider());
        }
        try {
            if (mapper.retryFailed(user.getTenantId(), runId, user.getId(), UUID.randomUUID().toString(), oldRun.getProvider()) != 1) {
                throw new IllegalStateException("失敗状態または接続状態を確認してください。");
            }
        } catch (DataIntegrityViolationException e) {
            throw new AccountingSyncAlreadyRunningException();
        }
        var run = mapper.findRun(user.getTenantId(), runId);
        auditService.record("RETRY_SYNC", "ACCOUNTING_SYNC_RUN", runId, "会計同期を再実行");
        return run;
    }

    @Transactional
    public AccountingSyncRun claimNext(List<String> providers) {
        if (providers.isEmpty()) return null;
        mapper.recoverExpiredRuns();
        var run = mapper.claimable(providers);
        if (run == null) return null;
        if (mapper.markRunning(run) != 1) return null;
        run.setAttemptCount(run.getAttemptCount() + 1);
        run.setStatus("RUNNING");
        return run;
    }

    @Transactional
    public void recordPage(long runId, long tenantId, long connectionId, int fetchedCount,
                           List<ExternalActualRecord> records, String nextCursor) {
        if (records == null || fetchedCount < records.size() || fetchedCount < 0) {
            throw new IllegalArgumentException("同期ページがありません。");
        }
        if (!mapper.runOwnsConnection(runId, tenantId, connectionId)) {
            throw new IllegalStateException("同期実行と接続の組み合わせが無効です。");
        }
        for (ExternalActualRecord record : records) {
            validateRecord(record);
            if (!mapper.dimensionsBelongToTenant(tenantId, record.categoryId(), record.departmentId(), record.projectId())) {
                throw new IllegalArgumentException("同期データのマスタ参照先がテナント内にありません。");
            }
            if (mapper.upsertActual(tenantId, connectionId, record) != 1) {
                throw new IllegalStateException("同期対象の接続が無効です。");
            }
        }
        if (mapper.recordPage(runId, tenantId, fetchedCount, records.size(), nextCursor) != 1) {
            throw new IllegalStateException("同期実行が有効な状態ではありません。");
        }
    }

    private void validateRecord(ExternalActualRecord record) {
        if (record == null || record.externalId() == null || record.externalId().isBlank() || record.externalId().length() > 240
                || record.bookedOn() == null || record.amount() == null
                || (record.description() != null && record.description().length() > 500)
                || (record.payloadHash() != null && record.payloadHash().length() > 64)) {
            throw new IllegalArgumentException("会計実績データの形式が不正です。");
        }
    }

    @Transactional
    public void complete(long runId, long tenantId, AccountingSyncResult result) {
        String status = result.partialSuccess() ? "PARTIAL_SUCCESS" : "SUCCEEDED";
        String message = result.partialSuccess() ? "一部の明細を取り込めませんでした。同期データの確認が必要です。" : null;
        if (mapper.finish(runId, tenantId, status, message, null) != 1) {
            throw new IllegalStateException("同期実行の完了状態を保存できません。");
        }
    }

    @Transactional
    public void fail(long runId, long tenantId, int attempt, int maxAttempts, boolean retryable) {
        if (retryable && attempt < maxAttempts) {
            long delaySeconds = Math.min(900, 15L * (1L << Math.min(attempt - 1, 6)));
            mapper.finish(runId, tenantId, "RETRY_WAIT", "一時的な同期エラーです。再試行を待っています。",
                    LocalDateTime.now().plusSeconds(delaySeconds));
        } else {
            mapper.finish(runId, tenantId, "FAILED", "同期を完了できませんでした。接続設定と同期履歴を確認してください。", null);
        }
    }

}
