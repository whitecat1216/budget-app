package com.example.budget.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface NotificationMapper {
    @Insert("INSERT INTO notifications (tenant_id, recipient_id, type, title, message, target_url) " +
            "SELECT u.tenant_id, u.id, #{type}, #{title}, #{message}, #{targetUrl} FROM users u " +
            "WHERE u.tenant_id = #{tenantId} AND u.enabled = TRUE AND u.id <> #{excludeUserId} " +
            "AND (u.role IN ('TENANT_ADMIN','FINANCE_ADMIN','APPROVER') OR " +
            "(u.role = 'DEPARTMENT_MANAGER' AND EXISTS (SELECT 1 FROM user_scopes s WHERE s.tenant_id=u.tenant_id " +
            "AND s.user_id=u.id AND s.scope_type='DEPARTMENT' AND s.scope_id=#{departmentId})) OR " +
            "(u.role = 'PROJECT_MANAGER' AND EXISTS (SELECT 1 FROM user_scopes s WHERE s.tenant_id=u.tenant_id " +
            "AND s.user_id=u.id AND s.scope_type='PROJECT' AND s.scope_id=#{projectId})))")
    int insertForReviewers(@Param("tenantId") long tenantId, @Param("excludeUserId") long excludeUserId,
                           @Param("departmentId") Long departmentId, @Param("projectId") Long projectId,
                           @Param("type") String type, @Param("title") String title,
                           @Param("message") String message, @Param("targetUrl") String targetUrl);

    @Insert("INSERT INTO notifications (tenant_id, recipient_id, type, title, message, target_url) " +
            "SELECT #{tenantId}, u.id, #{type}, #{title}, #{message}, #{targetUrl} FROM users u " +
            "WHERE u.tenant_id = #{tenantId} AND u.id = #{recipientId} AND u.enabled = TRUE")
    int insertForRecipient(@Param("tenantId") long tenantId, @Param("recipientId") long recipientId,
                           @Param("type") String type, @Param("title") String title,
                           @Param("message") String message, @Param("targetUrl") String targetUrl);

    @Select("SELECT id, type, title, message, target_url, read_at, " +
            "TO_CHAR(created_at, 'YYYY-MM-DD HH24:MI') AS created_at FROM notifications " +
            "WHERE tenant_id=#{tenantId} AND recipient_id=#{recipientId} ORDER BY created_at DESC, id DESC LIMIT 100")
    List<Map<String, Object>> findRecent(@Param("tenantId") long tenantId, @Param("recipientId") long recipientId);

    @Select("SELECT COUNT(*) FROM notifications WHERE tenant_id=#{tenantId} AND recipient_id=#{recipientId} AND read_at IS NULL")
    long countUnread(@Param("tenantId") long tenantId, @Param("recipientId") long recipientId);

    @Update("UPDATE notifications SET read_at=CURRENT_TIMESTAMP WHERE id=#{id} AND tenant_id=#{tenantId} AND recipient_id=#{recipientId} AND read_at IS NULL")
    int markRead(@Param("id") long id, @Param("tenantId") long tenantId, @Param("recipientId") long recipientId);

    @Update("UPDATE notifications SET read_at=CURRENT_TIMESTAMP WHERE tenant_id=#{tenantId} AND recipient_id=#{recipientId} AND read_at IS NULL")
    int markAllRead(@Param("tenantId") long tenantId, @Param("recipientId") long recipientId);
}
