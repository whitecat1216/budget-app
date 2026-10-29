package com.example.budget.mapper;

import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface AuditMapper {
    @Insert("INSERT INTO audit_logs (tenant_id, actor_id, action, entity_type, entity_id, details) VALUES (#{tenantId}, #{actorId}, #{action}, #{entityType}, #{entityId}, #{details})")
    int insert(@Param("tenantId") long tenantId, @Param("actorId") Long actorId,
               @Param("action") String action, @Param("entityType") String entityType,
               @Param("entityId") String entityId, @Param("details") String details);

    @Select("SELECT l.id, l.actor_id, u.display_name AS actor_name, l.action, l.entity_type, l.entity_id, l.details, " +
            "TO_CHAR(l.created_at, 'YYYY-MM-DD HH24:MI:SS') AS created_at FROM audit_logs l " +
            "LEFT JOIN users u ON u.id = l.actor_id AND u.tenant_id = l.tenant_id " +
            "WHERE l.tenant_id = #{tenantId} ORDER BY l.created_at DESC, l.id DESC LIMIT 200")
    List<Map<String, Object>> findRecent(@Param("tenantId") long tenantId);
}
