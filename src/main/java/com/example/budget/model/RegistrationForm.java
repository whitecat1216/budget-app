package com.example.budget.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegistrationForm {
    @NotBlank @Size(max = 160) private String companyName;
    @NotBlank @Size(max = 120) private String displayName;
    @NotBlank @Email @Size(max = 254) private String email;
    @NotBlank @Size(min = 12, max = 72) private String password;
    private boolean termsAccepted;
}
