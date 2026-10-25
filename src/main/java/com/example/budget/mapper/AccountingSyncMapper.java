package com.example.budget.mapper;

import com.example.budget.model.AccountingConnectionInfo;
import com.example.budget.model.AccountingSyncRun;
import com.example.budget.model.ExternalActualRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

@Mapper
public interface AccountingSyncMapper {
    @Select("SELECT id, tenant_id, provider, display_name, status FROM accounting_connections WHERE id = #{connectionId} AND tenant_id = #{tenantId}")
    AccountingConnectionInfo findConnection(@Param("tenantId") long tenantId, @Param("connectionId") long connectionId);

    @Select("SELECT id, tenant_id, provider, display_name, status FROM accounting_connections WHERE tenant_id = #{tenantId} ORDER BY display_name, id")
    List<AccountingConnectionInfo> findConnections(@Param("tenantId") long tenantId);

    @Update("UPDATE accounting_sync_runs SET status = CASE WHEN attempt_count >= max_attempts THEN 'FAILED' ELSE 'RETRY_WAIT' END, " +
            "next_attempt_at = CASE WHEN attempt_count >= max_attempts THEN NULL ELSE CURRENT_TIMESTAMP END, " +
            "error_message = 'ワーカーが停止したため同期を再開します。', worker_lease_until = NULL, " +
            "finished_at = CASE WHEN attempt_count >= max_attempts THEN CURRENT_TIMESTAMP ELSE NULL END, updated_at = CURRENT_TIMESTAMP " +
            "WHERE status = 'RUNNING' AND worker_lease_until < CURRENT_TIMESTAMP")
    int recoverExpiredRuns();

    @Select("SELECT COUNT(*) > 0 FROM accounting_sync_runs WHERE id = #{runId} AND tenant_id = #{tenantId} " +
            "AND connection_id = #{connectionId} AND status = 'RUNNING'")
    boolean runOwnsConnection(@Param("runId") long runId, @Param("tenantId") long tenantId,
                             @Param("connectionId") long connectionId);

    @Select("SELECT (#{categoryId,jdbcType=BIGINT} IS NULL OR EXISTS (SELECT 1 FROM categories WHERE id = #{categoryId,jdbcType=BIGINT} AND tenant_id = #{tenantId} AND type = 'EXPENSE')) " +
            "AND (#{departmentId,jdbcType=BIGINT} IS NULL OR EXISTS (SELECT 1 FROM departments WHERE id = #{departmentId,jdbcType=BIGINT} AND tenant_id = #{tenantId})) " +
            "AND (#{projectId,jdbcType=BIGINT} IS NULL OR EXISTS (SELECT 1 FROM projects WHERE id = #{projectId,jdbcType=BIGINT} AND tenant_id = #{tenantId} " +
            "AND (#{departmentId,jdbcType=BIGINT} IS NULL OR department_id = #{departmentId,jdbcType=BIGINT})))")
    boolean dimensionsBelongToTenant(@Param("tenantId") long tenantId, @Param("categoryId") Long categoryId,
                                    @Param("departmentId") Long departmentId, @Param("projectId") Long projectId);

    @Insert("INSERT INTO external_actuals (tenant_id, connection_id, external_id, category_id, department_id, project_id, booked_on, amount, description, payload_hash) " +
            "SELECT #{tenantId}, c.id, #{record.externalId}, #{record.categoryId}, #{record.departmentId}, #{record.projectId}, " +
            "#{record.bookedOn}, #{record.amount}, #{record.description}, #{record.payloadHash} FROM accounting_connections c " +
            "WHERE c.id = #{connectionId} AND c.tenant_id = #{tenantId} AND c.status = 'CONNECTED' " +
            "ON CONFLICT (connection_id, external_id) DO UPDATE SET category_id = COALESCE(EXCLUDED.category_id, external_actuals.category_id), " +
            "department_id = COALESCE(EXCLUDED.department_id, external_actuals.department_id), " +
            "project_id = COALESCE(EXCLUDED.project_id, external_actuals.project_id), booked_on = EXCLUDED.booked_on, amount = EXCLUDED.amount, description = EXCLUDED.description, " +
            "payload_hash = EXCLUDED.payload_hash, imported_at = CURRENT_TIMESTAMP WHERE external_actuals.tenant_id = EXCLUDED.tenant_id")
    int upsertActual(@Param("tenantId") long tenantId, @Param("connectionId") long connectionId,
                     @Param("record") ExternalActualRecord record);

    @Insert("INSERT INTO accounting_sync_runs (tenant_id, connection_id, requested_by, correlation_id, status) " +
            "SELECT #{tenantId}, c.id, #{requestedBy}, #{correlationId}, 'QUEUED' FROM accounting_connections c " +
            "WHERE c.id = #{connectionId} AND c.tenant_id = #{tenantId} AND c.status = 'CONNECTED'")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int enqueue(AccountingSyncRun run);

    @Select("SELECT r.id, r.tenant_id, r.connection_id, r.requested_by, r.correlation_id, r.status, r.fetched_count, r.imported_count, " +
            "r.attempt_count, r.max_attempts, r.cursor_value, r.error_message, r.queued_at, r.started_at, r.finished_at, r.next_attempt_at, c.provider " +
            "FROM accounting_sync_runs r JOIN accounting_connections c ON c.id = r.connection_id AND c.tenant_id = r.tenant_id " +
            "WHERE r.tenant_id = #{tenantId} ORDER BY r.queued_at DESC, r.id DESC LIMIT 100")
    List<AccountingSyncRun> findRecent(@Param("tenantId") long tenantId);

    @Select("SELECT r.id, r.tenant_id, r.connection_id, r.requested_by, r.correlation_id, r.status, r.fetched_count, r.imported_count, " +
            "r.attempt_count, r.max_attempts, r.cursor_value, r.error_message, r.queued_at, r.started_at, r.finished_at, r.next_attempt_at, c.provider " +
            "FROM accounting_sync_runs r JOIN accounting_connections c ON c.id = r.connection_id AND c.tenant_id = r.tenant_id " +
            "WHERE r.id = #{runId} AND r.tenant_id = #{tenantId}")
    AccountingSyncRun findRun(@Param("tenantId") long tenantId, @Param("runId") long runId);

    @Update("UPDATE accounting_sync_runs r SET status = 'QUEUED', requested_by = #{requestedBy}, correlation_id = #{correlationId}, " +
            "attempt_count = 0, next_attempt_at = NULL, error_message = NULL, queued_at = CURRENT_TIMESTAMP, " +
            "started_at = NULL, finished_at = NULL, worker_lease_until = NULL, updated_at = CURRENT_TIMESTAMP " +
            "FROM accounting_connections c WHERE r.id = #{runId} AND r.tenant_id = #{tenantId} AND r.status IN ('FAILED', 'PARTIAL_SUCCESS') " +
            "AND c.id = r.connection_id AND c.tenant_id = r.tenant_id AND c.status = 'CONNECTED' " +
            "AND c.provider = #{provider}")
    int retryFailed(@Param("tenantId") long tenantId, @Param("runId") long runId,
                    @Param("requestedBy") long requestedBy, @Param("correlationId") String correlationId,
                    @Param("provider") String provider);

    @Select("<script>SELECT r.id, r.tenant_id, r.connection_id, r.requested_by, r.correlation_id, r.status, r.fetched_count, " +
            "r.imported_count, r.attempt_count, r.max_attempts, r.cursor_value, r.error_message, r.queued_at, " +
            "r.started_at, r.finished_at, r.next_attempt_at, c.provider " +
            "FROM accounting_sync_runs r JOIN accounting_connections c ON c.id = r.connection_id AND c.tenant_id = r.tenant_id " +
            "WHERE r.status IN ('QUEUED', 'RETRY_WAIT') AND (r.next_attempt_at IS NULL OR r.next_attempt_at &lt;= CURRENT_TIMESTAMP) " +
            "AND c.status = 'CONNECTED' AND c.provider IN (" +
            "<foreach collection='providers' item='provider' separator=','>#{provider}</foreach>) " +
            "ORDER BY r.queued_at, r.id LIMIT 1 FOR UPDATE OF r SKIP LOCKED</script>")
    AccountingSyncRun claimable(@Param("providers") List<String> providers);

    @Update("UPDATE accounting_sync_runs SET status = 'RUNNING', attempt_count = attempt_count + 1, " +
            "started_at = COALESCE(started_at, CURRENT_TIMESTAMP), worker_lease_until = CURRENT_TIMESTAMP + INTERVAL '30 minutes', " +
            "finished_at = NULL, error_message = NULL, updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = #{id} AND tenant_id = #{tenantId} AND status IN ('QUEUED', 'RETRY_WAIT')")
    int markRunning(AccountingSyncRun run);

    @Update("UPDATE accounting_sync_runs SET fetched_count = fetched_count + #{fetched}, imported_count = imported_count + #{imported}, " +
            "cursor_value = #{cursor}, worker_lease_until = CURRENT_TIMESTAMP + INTERVAL '30 minutes', updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = #{runId} AND tenant_id = #{tenantId} AND status = 'RUNNING'")
    int recordPage(@Param("runId") long runId, @Param("tenantId") long tenantId,
                   @Param("fetched") int fetched, @Param("imported") int imported, @Param("cursor") String cursor);

    @Update("UPDATE accounting_sync_runs SET status = #{status}, error_message = #{message}, next_attempt_at = #{nextAttemptAt}, worker_lease_until = NULL, " +
            "finished_at = CASE WHEN #{status} = 'RETRY_WAIT' THEN NULL ELSE CURRENT_TIMESTAMP END, updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = #{runId} AND tenant_id = #{tenantId} AND status = 'RUNNING'")
    int finish(@Param("runId") long runId, @Param("tenantId") long tenantId,
               @Param("status") String status, @Param("message") String message,
               @Param("nextAttemptAt") java.time.LocalDateTime nextAttemptAt);
}
