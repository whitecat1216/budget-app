package com.example.budget.mapper;

import com.example.budget.model.Transaction;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface TransactionMapper {

    @Select("SELECT t.*, c.name as category_name FROM transactions t " +
            "LEFT JOIN categories c ON t.category_id = c.id " +
            "ORDER BY t.transaction_date DESC, t.id DESC")
    List<Transaction> findAll();

    @Select("SELECT t.*, c.name as category_name FROM transactions t " +
            "LEFT JOIN categories c ON t.category_id = c.id " +
            "WHERE t.id = #{id}")
    Transaction findById(Long id);

    @Select("SELECT t.*, c.name as category_name FROM transactions t " +
            "LEFT JOIN categories c ON t.category_id = c.id " +
            "WHERE t.transaction_date BETWEEN #{start} AND #{end} " +
            "ORDER BY t.transaction_date DESC")
    List<Transaction> findByDateRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Select("SELECT t.*, c.name as category_name FROM transactions t " +
            "LEFT JOIN categories c ON t.category_id = c.id " +
            "WHERE t.category_id = #{categoryId} " +
            "ORDER BY t.transaction_date DESC")
    List<Transaction> findByCategory(Long categoryId);

    @Insert("INSERT INTO transactions (category_id, type, amount, description, transaction_date, created_at, updated_at) " +
            "VALUES (#{categoryId}, #{type}, #{amount}, #{description}, #{transactionDate}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Transaction transaction);

    @Update("UPDATE transactions SET category_id = #{categoryId}, type = #{type}, amount = #{amount}, " +
            "description = #{description}, transaction_date = #{transactionDate}, updated_at = NOW() " +
            "WHERE id = #{id}")
    int update(Transaction transaction);

    @Delete("DELETE FROM transactions WHERE id = #{id}")
    int delete(Long id);

    @Select("SELECT COALESCE(SUM(amount), 0) FROM transactions " +
            "WHERE type = #{type} AND transaction_date BETWEEN #{start} AND #{end}")
    BigDecimal sumByTypeAndDateRange(@Param("type") String type,
                                     @Param("start") LocalDate start,
                                     @Param("end") LocalDate end);

    @Select("SELECT COALESCE(SUM(amount), 0) FROM transactions " +
            "WHERE category_id = #{categoryId} AND type = 'EXPENSE' " +
            "AND EXTRACT(YEAR FROM transaction_date) = #{year} " +
            "AND EXTRACT(MONTH FROM transaction_date) = #{month}")
    BigDecimal sumExpenseByCategoryAndMonth(@Param("categoryId") Long categoryId,
                                            @Param("year") int year,
                                            @Param("month") int month);

    @Select("SELECT c.name as category, COALESCE(SUM(t.amount), 0) as total " +
            "FROM transactions t JOIN categories c ON t.category_id = c.id " +
            "WHERE t.type = 'EXPENSE' AND t.transaction_date BETWEEN #{start} AND #{end} " +
            "GROUP BY c.id, c.name ORDER BY total DESC")
    List<Map<String, Object>> expenseSummaryByCategory(@Param("start") LocalDate start,
                                                       @Param("end") LocalDate end);
}
