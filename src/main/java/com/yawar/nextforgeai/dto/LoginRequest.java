package com.yawar.nextforgeai.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "'email' can't be Empty or Blank !")
    @Email(message = "'email' should be valid")
    private String email;

    @NotBlank(message = "'password' can't be Empty or Blank !")
    private String password;
}
