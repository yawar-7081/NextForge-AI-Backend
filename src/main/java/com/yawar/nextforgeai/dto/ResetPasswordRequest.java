package com.yawar.nextforgeai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank(message = "'token' can't be Empty or Blank !")
    private String token;

    @NotBlank(message = "'newPassword' can't be Empty or Blank !")
    private String newPassword;
}