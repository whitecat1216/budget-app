package com.example.budget.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ApprovalWorkflowMapper {
    @Insert("<script>INSERT INTO spending_request_approvals (tenant_id, request_id, approval_round, step_number, required_role) VALUES " +
            "<foreach collection='roles' item='role' index='i' separator=','>(#{tenantId}, #{requestId}, #{round}, #{i}+1, #{role})</foreach></script>")
    int insertStages(@Param("tenantId") long tenantId, @Param("requestId") long requestId,
                     @Param("round") int round, @Param("roles") List<String> roles);

    @Select("SELECT required_role FROM spending_request_approvals WHERE tenant_id=#{tenantId} AND request_id=#{requestId} " +
            "AND approval_round=#{round} AND step_number=#{stepNumber} AND status='PENDING'")
    String currentRole(@Param("tenantId") long tenantId, @Param("requestId") long requestId,
                       @Param("round") int round, @Param("stepNumber") int stepNumber);

    @Select("SELECT COUNT(*) > 0 FROM spending_request_approvals WHERE tenant_id=#{tenantId} AND request_id=#{requestId} " +
            "AND approval_round=#{round} AND step_number=#{stepNumber}")
    boolean hasStep(@Param("tenantId") long tenantId, @Param("requestId") long requestId,
                    @Param("round") int round, @Param("stepNumber") int stepNumber);

    @Update("UPDATE spending_request_approvals SET status='APPROVED', reviewer_id=#{reviewerId}, reviewed_at=CURRENT_TIMESTAMP " +
            "WHERE tenant_id=#{tenantId} AND request_id=#{requestId} AND approval_round=#{round} " +
            "AND step_number=#{stepNumber} AND required_role=#{role} AND status='PENDING' " +
            "AND NOT EXISTS (SELECT 1 FROM spending_request_approvals prior WHERE prior.tenant_id=#{tenantId} " +
            "AND prior.request_id=#{requestId} AND prior.approval_round=#{round} AND prior.reviewer_id=#{reviewerId})")
    int approveStage(@Param("tenantId") long tenantId, @Param("requestId") long requestId,
                     @Param("round") int round, @Param("stepNumber") int stepNumber,
                     @Param("reviewerId") long reviewerId, @Param("role") String role);

    @Update("UPDATE spending_request_approvals SET status='SKIPPED' WHERE tenant_id=#{tenantId} AND request_id=#{requestId} " +
            "AND approval_round=#{round} AND status='PENDING'")
    int skipPending(@Param("tenantId") long tenantId, @Param("requestId") long requestId, @Param("round") int round);

    @Update("UPDATE spending_requests SET current_approval_step=#{nextStep}, updated_at=CURRENT_TIMESTAMP " +
            "WHERE tenant_id=#{tenantId} AND id=#{requestId} AND approval_round=#{round} " +
            "AND current_approval_step=#{currentStep} AND status='SUBMITTED'")
    int advance(@Param("tenantId") long tenantId, @Param("requestId") long requestId, @Param("round") int round,
                @Param("currentStep") int currentStep, @Param("nextStep") int nextStep);

    @Update("UPDATE spending_requests SET status='APPROVED', updated_at=CURRENT_TIMESTAMP " +
            "WHERE tenant_id=#{tenantId} AND id=#{requestId} AND approval_round=#{round} " +
            "AND current_approval_step=#{currentStep} AND status='SUBMITTED'")
    int complete(@Param("tenantId") long tenantId, @Param("requestId") long requestId,
                 @Param("round") int round, @Param("currentStep") int currentStep);
}
