package com.example.budget.mapper;

import com.example.budget.model.Category;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CategoryMapper {

    @Select("SELECT * FROM categories ORDER BY name")
    List<Category> findAll();

    @Select("SELECT * FROM categories WHERE id = #{id}")
    Category findById(Long id);

    @Select("SELECT * FROM categories WHERE type = #{type} ORDER BY name")
    List<Category> findByType(String type);

    @Insert("INSERT INTO categories (name, type, color, icon, created_at, updated_at) " +
            "VALUES (#{name}, #{type}, #{color}, #{icon}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Category category);

    @Update("UPDATE categories SET name = #{name}, type = #{type}, color = #{color}, " +
            "icon = #{icon}, updated_at = NOW() WHERE id = #{id}")
    int update(Category category);

    @Delete("DELETE FROM categories WHERE id = #{id}")
    int delete(Long id);
}
