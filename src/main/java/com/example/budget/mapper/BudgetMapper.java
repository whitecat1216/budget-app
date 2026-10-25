package com.example.budget.mapper;

import com.example.budget.model.Budget;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface BudgetMapper {

    @Select("SELECT b.*, c.name as category_name, d.name as department_name, p.name as project_name FROM budgets b " +
            "LEFT JOIN categories c ON b.category_id = c.id AND c.tenant_id=b.tenant_id LEFT JOIN departments d ON b.department_id = d.id AND d.tenant_id=b.tenant_id LEFT JOIN projects p ON b.project_id = p.id AND p.tenant_id=b.tenant_id " +
            "WHERE b.tenant_id = #{tenantId} ORDER BY b.year DESC, b.month DESC, c.name")
    List<Budget> findAll(@Param("tenantId") long tenantId);

    @Select("SELECT b.*, c.name as category_name, d.name as department_name, p.name as project_name FROM budgets b " +
            "LEFT JOIN categories c ON b.category_id = c.id AND c.tenant_id=b.tenant_id LEFT JOIN departments d ON b.department_id = d.id AND d.tenant_id=b.tenant_id LEFT JOIN projects p ON b.project_id = p.id AND p.tenant_id=b.tenant_id " +
            "WHERE b.id = #{id} AND b.tenant_id = #{tenantId}")
    Budget findById(@Param("id") Long id, @Param("tenantId") long tenantId);

    @Select("SELECT b.*, c.name as category_name, d.name as department_name, p.name as project_name FROM budgets b " +
            "LEFT JOIN categories c ON b.category_id = c.id AND c.tenant_id=b.tenant_id LEFT JOIN departments d ON b.department_id = d.id AND d.tenant_id=b.tenant_id LEFT JOIN projects p ON b.project_id = p.id AND p.tenant_id=b.tenant_id " +
            "WHERE b.year = #{year} AND b.month = #{month} AND b.tenant_id = #{tenantId} " +
            "ORDER BY c.name")
    List<Budget> findByYearMonth(@Param("year") int year, @Param("month") int month, @Param("tenantId") long tenantId);

    @Select("SELECT b.*, c.name as category_name, d.name as department_name, p.name as project_name FROM budgets b " +
            "LEFT JOIN categories c ON b.category_id = c.id AND c.tenant_id=b.tenant_id LEFT JOIN departments d ON b.department_id = d.id AND d.tenant_id=b.tenant_id LEFT JOIN projects p ON b.project_id = p.id AND p.tenant_id=b.tenant_id " +
            "WHERE b.year = #{year} AND b.month = #{month} AND b.tenant_id = #{tenantId} " +
            "AND (CAST(#{departmentId} AS BIGINT) IS NULL OR b.department_id = #{departmentId}) " +
            "AND (CAST(#{projectId} AS BIGINT) IS NULL OR b.project_id = #{projectId}) ORDER BY c.name")
    List<Budget> findByYearMonthAndScope(@Param("year") int year, @Param("month") int month,
                                         @Param("departmentId") Long departmentId,
                                         @Param("projectId") Long projectId,
                                         @Param("tenantId") long tenantId);

    @Insert("INSERT INTO budgets (tenant_id, category_id, department_id, project_id, year, month, amount, created_at, updated_at) " +
            "VALUES (#{tenantId}, #{categoryId}, #{departmentId}, #{projectId}, #{year}, #{month}, #{amount}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Budget budget);

}
