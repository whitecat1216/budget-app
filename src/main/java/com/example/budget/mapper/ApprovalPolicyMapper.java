package com.example.budget.mapper;

import com.example.budget.model.ApprovalPolicy;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface ApprovalPolicyMapper {
    @Select("SELECT * FROM approval_policies WHERE tenant_id=#{tenantId} ORDER BY min_amount, policy_name")
    List<ApprovalPolicy> findAll(@Param("tenantId") long tenantId);

    @Select("SELECT id FROM tenants WHERE id=#{tenantId} FOR UPDATE")
    Long lockTenant(@Param("tenantId") long tenantId);

    @Select("SELECT COUNT(*) FROM approval_policies WHERE tenant_id=#{tenantId} AND enabled=TRUE " +
            "AND #{minAmount} < COALESCE(max_amount, 999999999999.99) " +
            "AND min_amount < COALESCE(#{maxAmount}, 999999999999.99)")
    int countOverlaps(@Param("tenantId") long tenantId, @Param("minAmount") BigDecimal minAmount,
                      @Param("maxAmount") BigDecimal maxAmount);

    @Insert("INSERT INTO approval_policies (tenant_id, policy_name, min_amount, max_amount, approval_roles) " +
            "VALUES (#{tenantId}, #{policyName}, #{minAmount}, #{maxAmount}, #{approvalRoles})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ApprovalPolicy policy);

    @Update("UPDATE approval_policies SET enabled=NOT enabled WHERE id=#{id} AND tenant_id=#{tenantId}")
    int toggleEnabled(@Param("tenantId") long tenantId, @Param("id") long id);

    @Select("SELECT approval_roles FROM approval_policies WHERE tenant_id=#{tenantId} AND enabled=TRUE " +
            "AND #{amount} >= min_amount AND (max_amount IS NULL OR #{amount} < max_amount) ORDER BY id LIMIT 1")
    String findRolesForAmount(@Param("tenantId") long tenantId, @Param("amount") BigDecimal amount);

    @Select("SELECT COUNT(*) FROM approval_policies WHERE tenant_id=#{tenantId} AND enabled=TRUE")
    int countEnabled(@Param("tenantId") long tenantId);
}
