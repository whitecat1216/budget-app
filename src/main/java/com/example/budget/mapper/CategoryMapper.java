package com.example.budget.mapper;

import com.example.budget.model.Category;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CategoryMapper {

    @Select("SELECT * FROM categories WHERE tenant_id = #{tenantId} ORDER BY name")
    List<Category> findAll(@Param("tenantId") long tenantId);

    @Select("SELECT * FROM categories WHERE id = #{id} AND tenant_id = #{tenantId}")
    Category findById(@Param("id") Long id, @Param("tenantId") long tenantId);

    @Select("SELECT * FROM categories WHERE type = #{type} AND tenant_id = #{tenantId} ORDER BY name")
    List<Category> findByType(@Param("type") String type, @Param("tenantId") long tenantId);

    @Insert("INSERT INTO categories (tenant_id, name, type, color, icon, created_at, updated_at) " +
            "VALUES (#{tenantId}, #{name}, #{type}, #{color}, #{icon}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Category category);

    @Update("UPDATE categories SET name = #{name}, type = #{type}, color = #{color}, " +
            "icon = #{icon}, updated_at = NOW() WHERE id = #{id} AND tenant_id = #{tenantId}")
    int update(Category category);

    @Delete("DELETE FROM categories WHERE id = #{id} AND tenant_id = #{tenantId}")
    int delete(@Param("id") Long id, @Param("tenantId") long tenantId);

    @Insert("INSERT INTO categories (tenant_id, name, type, color, icon) SELECT #{tenantId}, name, type, color, icon FROM categories WHERE tenant_id = 1")
    int copyDefaultCategories(@Param("tenantId") long tenantId);
}
