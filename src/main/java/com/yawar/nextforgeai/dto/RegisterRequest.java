package com.yawar.nextforgeai.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "'name' can't be Empty or Blank !")
    @Size(min = 3, max = 50, message = "'name' length should be between 3 to 50")
    private String name;

    @NotBlank(message = "'email' can't be Empty or Blank !")
    @Email(message = "'email' should be valid")
    private String email;

    @NotBlank(message = "'password' can't be Empty or Blank !")
    private String password;
}
