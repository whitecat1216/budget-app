package com.example.budget.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InvitationForm {
    @NotBlank @Email @Size(max=254) private String email;
    @NotBlank @Size(max=120) private String displayName;
    @NotBlank private String role = "MEMBER";
}
