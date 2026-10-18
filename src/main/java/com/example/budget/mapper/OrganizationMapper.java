package com.example.budget.mapper;

import com.example.budget.model.Department;
import com.example.budget.model.Project;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface OrganizationMapper {
    @Select("SELECT * FROM departments WHERE tenant_id = #{tenantId} ORDER BY name")
    List<Department> findDepartments(@Param("tenantId") long tenantId);

    @Insert("INSERT INTO departments (tenant_id, code, name, manager) VALUES (#{tenantId}, #{code}, #{name}, #{manager})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertDepartment(Department department);

    @Select("SELECT p.*, d.name AS department_name FROM projects p LEFT JOIN departments d ON d.id = p.department_id AND d.tenant_id = p.tenant_id WHERE p.tenant_id = #{tenantId} ORDER BY p.created_at DESC")
    List<Project> findProjects(@Param("tenantId") long tenantId);

    @Insert("INSERT INTO projects (tenant_id, department_id, code, name, owner, start_date, end_date) VALUES (#{tenantId}, #{departmentId}, #{code}, #{name}, #{owner}, #{startDate}, #{endDate})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertProject(Project project);

    @Select("SELECT COUNT(*) > 0 FROM departments WHERE id = #{id} AND tenant_id = #{tenantId}")
    boolean departmentBelongsToTenant(@Param("id") Long id, @Param("tenantId") long tenantId);

    @Select("SELECT COUNT(*) > 0 FROM projects WHERE id = #{id} AND tenant_id = #{tenantId}")
    boolean projectBelongsToTenant(@Param("id") Long id, @Param("tenantId") long tenantId);
}
