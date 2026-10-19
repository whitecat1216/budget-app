package com.example.budget.mapper;

import com.example.budget.model.BudgetChangeRequest;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface BudgetChangeRequestMapper {
    @Select("SELECT r.*, u.display_name AS requester_name, b.amount AS current_amount, b.year, b.month, b.department_id, b.project_id, c.name AS category_name FROM budget_change_requests r JOIN users u ON u.id=r.requester_id AND u.tenant_id=r.tenant_id JOIN budgets b ON b.id=r.budget_id AND b.tenant_id=r.tenant_id JOIN categories c ON c.id=b.category_id AND c.tenant_id=b.tenant_id WHERE r.tenant_id=#{tenantId} ORDER BY r.created_at DESC")
    List<BudgetChangeRequest> findAll(@Param("tenantId") long tenantId);

    @Select("SELECT r.*, b.amount AS current_amount FROM budget_change_requests r JOIN budgets b ON b.id=r.budget_id AND b.tenant_id=r.tenant_id WHERE r.id=#{id} AND r.tenant_id=#{tenantId}")
    BudgetChangeRequest findById(@Param("id") long id, @Param("tenantId") long tenantId);

    @Insert("INSERT INTO budget_change_requests (tenant_id, budget_id, requester_id, change_amount, reason, status) VALUES (#{tenantId}, #{budgetId}, #{requesterId}, #{changeAmount}, #{reason}, 'PENDING')")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BudgetChangeRequest request);

    @Update("UPDATE budget_change_requests SET status=#{status}, reviewer_id=#{reviewerId}, reviewed_at=CURRENT_TIMESTAMP, review_comment=#{comment} WHERE id=#{id} AND tenant_id=#{tenantId} AND status='PENDING' AND requester_id <> #{reviewerId}")
    int review(@Param("id") long id, @Param("tenantId") long tenantId, @Param("reviewerId") long reviewerId, @Param("status") String status, @Param("comment") String comment);

    @Update("UPDATE budgets SET amount=#{amount}, updated_at=CURRENT_TIMESTAMP WHERE id=#{budgetId} AND tenant_id=#{tenantId} AND amount=#{expectedAmount}")
    int updateBudgetAmount(@Param("budgetId") long budgetId, @Param("tenantId") long tenantId,
                           @Param("amount") java.math.BigDecimal amount, @Param("expectedAmount") java.math.BigDecimal expectedAmount);
}
