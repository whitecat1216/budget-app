package com.example.budget.mapper;

import com.example.budget.model.Budget;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface BudgetMapper {

    @Select("SELECT b.*, c.name as category_name FROM budgets b " +
            "LEFT JOIN categories c ON b.category_id = c.id " +
            "ORDER BY b.year DESC, b.month DESC, c.name")
    List<Budget> findAll();

    @Select("SELECT b.*, c.name as category_name FROM budgets b " +
            "LEFT JOIN categories c ON b.category_id = c.id " +
            "WHERE b.id = #{id}")
    Budget findById(Long id);

    @Select("SELECT b.*, c.name as category_name FROM budgets b " +
            "LEFT JOIN categories c ON b.category_id = c.id " +
            "WHERE b.year = #{year} AND b.month = #{month} " +
            "ORDER BY c.name")
    List<Budget> findByYearMonth(@Param("year") int year, @Param("month") int month);

    @Select("SELECT b.*, c.name as category_name FROM budgets b " +
            "LEFT JOIN categories c ON b.category_id = c.id " +
            "WHERE b.category_id = #{categoryId} AND b.year = #{year} AND b.month = #{month}")
    Budget findByCategoryAndYearMonth(@Param("categoryId") Long categoryId,
                                      @Param("year") int year,
                                      @Param("month") int month);

    @Insert("INSERT INTO budgets (category_id, year, month, amount, created_at, updated_at) " +
            "VALUES (#{categoryId}, #{year}, #{month}, #{amount}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Budget budget);

    @Update("UPDATE budgets SET category_id = #{categoryId}, year = #{year}, month = #{month}, " +
            "amount = #{amount}, updated_at = NOW() WHERE id = #{id}")
    int update(Budget budget);

    @Delete("DELETE FROM budgets WHERE id = #{id}")
    int delete(Long id);
}
