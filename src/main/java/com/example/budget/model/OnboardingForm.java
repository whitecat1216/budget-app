package com.example.budget.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OnboardingForm {
    @NotBlank
    @Size(max = 160)
    private String companyName;

    @Min(1)
    @Max(12)
    private int fiscalYearStartMonth = 4;

    @Pattern(regexp = "^[A-Z]{3}$")
    private String baseCurrency = "JPY";

    @NotBlank
    @Size(max = 64)
    private String timezone = "Asia/Tokyo";

    @Size(max = 40)
    private String departmentCode;

    @Size(max = 120)
    private String departmentName;
}
