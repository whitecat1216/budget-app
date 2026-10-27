package com.example.budget.mapper;

import com.example.budget.model.OnboardingForm;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OnboardingMapper {
    @Select("SELECT setup_completed FROM tenants WHERE id = #{tenantId}")
    Boolean isComplete(@Param("tenantId") long tenantId);

    @Select("SELECT name AS company_name, fiscal_year_start_month, base_currency, timezone FROM tenants WHERE id = #{tenantId}")
    OnboardingForm findSettings(@Param("tenantId") long tenantId);

    @Update("UPDATE tenants SET name = #{form.companyName}, fiscal_year_start_month = #{form.fiscalYearStartMonth}, base_currency = #{form.baseCurrency}, timezone = #{form.timezone}, setup_completed = TRUE WHERE id = #{tenantId} AND status = 'ACTIVE'")
    int complete(@Param("tenantId") long tenantId, @Param("form") OnboardingForm form);
}
