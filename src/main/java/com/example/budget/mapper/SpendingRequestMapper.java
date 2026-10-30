package com.example.budget.mapper;

import com.example.budget.model.SpendingRequest;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface SpendingRequestMapper {
    @Select("SELECT r.*, u.display_name AS requester_name, d.name AS department_name, p.name AS project_name, c.name AS category_name, " +
            "(SELECT count(*) FROM spending_request_approvals a WHERE a.tenant_id=r.tenant_id AND a.request_id=r.id AND a.approval_round=r.approval_round) AS approval_step_count, " +
            "(SELECT a.required_role FROM spending_request_approvals a WHERE a.tenant_id=r.tenant_id AND a.request_id=r.id AND a.approval_round=r.approval_round AND a.step_number=r.current_approval_step AND a.status='PENDING') AS current_approval_role " +
            "FROM spending_requests r JOIN users u ON u.id = r.requester_id AND u.tenant_id = r.tenant_id LEFT JOIN departments d ON d.id = r.department_id AND d.tenant_id = r.tenant_id LEFT JOIN projects p ON p.id = r.project_id AND p.tenant_id = r.tenant_id LEFT JOIN categories c ON c.id = r.category_id AND c.tenant_id = r.tenant_id WHERE r.tenant_id = #{tenantId} AND (#{admin} = TRUE OR r.requester_id = #{userId}) ORDER BY r.created_at DESC")
    List<SpendingRequest> findAll(@Param("tenantId") long tenantId, @Param("userId") long userId, @Param("admin") boolean admin);

    @Select("SELECT * FROM spending_requests WHERE id = #{id} AND tenant_id = #{tenantId}")
    SpendingRequest findById(@Param("id") long id, @Param("tenantId") long tenantId);

    @Insert("INSERT INTO spending_requests (tenant_id, requester_id, department_id, project_id, category_id, title, purpose, amount, requested_date, status) VALUES (#{tenantId}, #{requesterId}, #{departmentId}, #{projectId}, #{categoryId}, #{title}, #{purpose}, #{amount}, #{requestedDate}, 'DRAFT')")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SpendingRequest request);

    @Update("UPDATE spending_requests SET status = 'SUBMITTED', approval_round=approval_round+1, current_approval_step=1, updated_at = CURRENT_TIMESTAMP WHERE id = #{id} AND tenant_id = #{tenantId} AND requester_id = #{userId} AND status IN ('DRAFT','RETURNED')")
    int submit(@Param("id") long id, @Param("tenantId") long tenantId, @Param("userId") long userId);

    @Select("SELECT approval_round FROM spending_requests WHERE id=#{id} AND tenant_id=#{tenantId}")
    int approvalRound(@Param("id") long id, @Param("tenantId") long tenantId);

    @Update("UPDATE spending_requests SET status = 'APPROVED', updated_at = CURRENT_TIMESTAMP WHERE id = #{id} AND tenant_id = #{tenantId} AND requester_id <> #{userId} AND status = 'SUBMITTED'")
    int approve(@Param("id") long id, @Param("tenantId") long tenantId, @Param("userId") long userId);

    @Update("UPDATE spending_requests SET status = #{status}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id} AND tenant_id = #{tenantId} AND requester_id <> #{userId} AND status = 'SUBMITTED'")
    int finishReview(@Param("id") long id, @Param("tenantId") long tenantId, @Param("userId") long userId, @Param("status") String status);

    @Insert("INSERT INTO approval_history (tenant_id, request_id, actor_id, action, comment) VALUES (#{tenantId}, #{requestId}, #{actorId}, #{action}, #{comment})")
    int insertHistory(@Param("tenantId") long tenantId, @Param("requestId") long requestId,
                      @Param("actorId") long actorId, @Param("action") String action, @Param("comment") String comment);

    @Select("SELECT h.action, h.comment, h.created_at, u.display_name AS actor_name FROM approval_history h JOIN users u ON u.id = h.actor_id AND u.tenant_id = h.tenant_id WHERE h.tenant_id = #{tenantId} AND h.request_id = #{requestId} ORDER BY h.created_at, h.id")
    List<Map<String, Object>> findHistory(@Param("tenantId") long tenantId, @Param("requestId") long requestId);
}
