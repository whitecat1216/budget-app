package com.example.budget.mapper;

import com.example.budget.model.AccountCode;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AccountCodeMapper {
    @Select("SELECT a.*, c.name AS category_name FROM account_codes a LEFT JOIN categories c " +
            "ON c.id = a.category_id AND c.tenant_id = a.tenant_id " +
            "WHERE a.tenant_id = #{tenantId} ORDER BY a.code, a.id")
    List<AccountCode> findAll(@Param("tenantId") long tenantId);

    @Select("SELECT EXISTS(SELECT 1 FROM categories WHERE id = #{categoryId} AND tenant_id = #{tenantId} AND type = #{type})")
    boolean categoryMatches(@Param("categoryId") Long categoryId, @Param("tenantId") long tenantId,
                            @Param("type") String type);

    @Insert("INSERT INTO account_codes (tenant_id, code, name, type, category_id) " +
            "VALUES (#{tenantId}, #{code}, #{name}, #{type}, #{categoryId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AccountCode accountCode);

    @Update("UPDATE account_codes SET active = NOT active, updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = #{id} AND tenant_id = #{tenantId}")
    int toggleActive(@Param("id") Long id, @Param("tenantId") long tenantId);

    @Select("SELECT code FROM account_codes WHERE id = #{id} AND tenant_id = #{tenantId}")
    String findCode(@Param("id") Long id, @Param("tenantId") long tenantId);
}
