package com.yawar.nextforgeai.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequest {
    @NotBlank(message = "'email' can't be Empty or Blank !")
    @Email(message = "'email' should be valid")
    private String email;
}
