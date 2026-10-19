package com.example.budget.mapper;

import com.example.budget.model.Transaction;
import com.example.budget.model.CategoryExpenseSummary;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface TransactionMapper {

    @Select("SELECT t.*, c.name as category_name, d.name as department_name, p.name as project_name FROM transactions t " +
            "LEFT JOIN categories c ON t.category_id = c.id AND c.tenant_id = t.tenant_id LEFT JOIN departments d ON t.department_id = d.id AND d.tenant_id = t.tenant_id LEFT JOIN projects p ON t.project_id = p.id AND p.tenant_id = t.tenant_id " +
            "WHERE t.tenant_id = #{tenantId} ORDER BY t.transaction_date DESC, t.id DESC")
    List<Transaction> findAll(@Param("tenantId") long tenantId);

    @Select("SELECT t.*, c.name as category_name, d.name as department_name, p.name as project_name FROM transactions t " +
            "LEFT JOIN categories c ON t.category_id = c.id AND c.tenant_id = t.tenant_id LEFT JOIN departments d ON t.department_id = d.id AND d.tenant_id = t.tenant_id LEFT JOIN projects p ON t.project_id = p.id AND p.tenant_id = t.tenant_id " +
            "WHERE t.id = #{id} AND t.tenant_id = #{tenantId}")
    Transaction findById(@Param("id") Long id, @Param("tenantId") long tenantId);

    @Select("SELECT t.*, c.name as category_name, d.name as department_name, p.name as project_name FROM transactions t " +
            "LEFT JOIN categories c ON t.category_id = c.id AND c.tenant_id = t.tenant_id LEFT JOIN departments d ON t.department_id = d.id AND d.tenant_id = t.tenant_id LEFT JOIN projects p ON t.project_id = p.id AND p.tenant_id = t.tenant_id " +
            "WHERE t.transaction_date BETWEEN #{start} AND #{end} AND t.tenant_id = #{tenantId} " +
            "ORDER BY t.transaction_date DESC")
    List<Transaction> findByDateRange(@Param("start") LocalDate start, @Param("end") LocalDate end, @Param("tenantId") long tenantId);

    @Select("SELECT t.*, c.name as category_name FROM transactions t " +
            "LEFT JOIN categories c ON t.category_id = c.id AND c.tenant_id=t.tenant_id " +
            "WHERE t.category_id = #{categoryId} AND t.tenant_id = #{tenantId} " +
            "ORDER BY t.transaction_date DESC")
    List<Transaction> findByCategory(@Param("categoryId") Long categoryId, @Param("tenantId") long tenantId);

    @Insert("INSERT INTO transactions (tenant_id, created_by, category_id, department_id, project_id, type, amount, description, transaction_date, created_at, updated_at) " +
            "VALUES (#{tenantId}, #{createdBy}, #{categoryId}, #{departmentId}, #{projectId}, #{type}, #{amount}, #{description}, #{transactionDate}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Transaction transaction);

    @Update("UPDATE transactions SET category_id = #{categoryId}, department_id = #{departmentId}, project_id = #{projectId}, type = #{type}, amount = #{amount}, " +
            "description = #{description}, transaction_date = #{transactionDate}, updated_at = NOW() " +
            "WHERE id = #{id} AND tenant_id = #{tenantId}")
    int update(Transaction transaction);

    @Delete("DELETE FROM transactions WHERE id = #{id} AND tenant_id = #{tenantId}")
    int delete(@Param("id") Long id, @Param("tenantId") long tenantId);

    @Select("SELECT COALESCE(SUM(amount), 0) FROM transactions " +
            "WHERE type = #{type} AND transaction_date BETWEEN #{start} AND #{end} AND tenant_id = #{tenantId}")
    BigDecimal sumByTypeAndDateRange(@Param("type") String type,
                                     @Param("start") LocalDate start,
                                     @Param("end") LocalDate end,
                                     @Param("tenantId") long tenantId);

    @Select("SELECT COALESCE(SUM(amount), 0) FROM transactions " +
            "WHERE category_id = #{categoryId} AND type = 'EXPENSE' " +
            "AND EXTRACT(YEAR FROM transaction_date) = #{year} " +
            "AND EXTRACT(MONTH FROM transaction_date) = #{month} " +
            "AND department_id IS NOT DISTINCT FROM #{departmentId} " +
            "AND project_id IS NOT DISTINCT FROM #{projectId} AND tenant_id = #{tenantId}")
    BigDecimal sumExpenseByCategoryAndMonth(@Param("categoryId") Long categoryId,
                                            @Param("year") int year,
                                            @Param("month") int month,
                                            @Param("departmentId") Long departmentId,
                                            @Param("projectId") Long projectId,
                                            @Param("tenantId") long tenantId);

    @Select("SELECT c.name as category, COALESCE(SUM(t.amount), 0) as total " +
            "FROM transactions t JOIN categories c ON t.category_id = c.id AND c.tenant_id=t.tenant_id " +
            "WHERE t.type = 'EXPENSE' AND t.transaction_date BETWEEN #{start} AND #{end} AND t.tenant_id = #{tenantId} " +
            "GROUP BY c.id, c.name ORDER BY total DESC")
    List<Map<String, Object>> expenseSummaryByCategory(@Param("start") LocalDate start,
                                                       @Param("end") LocalDate end,
                                                       @Param("tenantId") long tenantId);

    @Select("SELECT c.id AS category_id, c.name AS category_name, COALESCE(SUM(t.amount), 0) AS actual " +
            "FROM categories c LEFT JOIN transactions t ON t.category_id = c.id " +
            "AND t.type = 'EXPENSE' AND t.transaction_date BETWEEN #{start} AND #{end} AND t.tenant_id = #{tenantId} " +
            "WHERE c.type = 'EXPENSE' AND c.tenant_id = #{tenantId} GROUP BY c.id, c.name ORDER BY actual DESC, c.name")
    @Results({
            @Result(property = "categoryId", column = "category_id"),
            @Result(property = "categoryName", column = "category_name"),
            @Result(property = "actual", column = "actual")
    })
    List<CategoryExpenseSummary> expenseSummaryByCategoryIncludingEmpty(@Param("start") LocalDate start,
                                                                        @Param("end") LocalDate end,
                                                                        @Param("tenantId") long tenantId);

    @Select("SELECT c.id AS category_id, c.name AS category_name, COALESCE(SUM(t.amount), 0) AS actual " +
            "FROM categories c LEFT JOIN transactions t ON t.category_id = c.id " +
            "AND t.type = 'EXPENSE' AND t.transaction_date BETWEEN #{start} AND #{end} " +
            "AND (CAST(#{departmentId} AS BIGINT) IS NULL OR t.department_id = #{departmentId}) " +
            "AND (CAST(#{projectId} AS BIGINT) IS NULL OR t.project_id = #{projectId}) AND t.tenant_id = #{tenantId} " +
            "WHERE c.type = 'EXPENSE' AND c.tenant_id = #{tenantId} GROUP BY c.id, c.name ORDER BY actual DESC, c.name")
    @Results({
            @Result(property = "categoryId", column = "category_id"),
            @Result(property = "categoryName", column = "category_name"),
            @Result(property = "actual", column = "actual")
    })
    List<CategoryExpenseSummary> expenseSummaryByCategoryAndScope(@Param("start") LocalDate start,
                                                                  @Param("end") LocalDate end,
                                                                  @Param("departmentId") Long departmentId,
                                                                  @Param("projectId") Long projectId,
                                                                  @Param("tenantId") long tenantId);
}
