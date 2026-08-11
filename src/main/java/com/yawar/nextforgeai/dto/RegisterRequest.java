package com.yawar.nextforgeai.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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

    @NotBlank(message = "Password can't be empty or blank.")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&^#()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,}$",
            message = "Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one number, and one special character."
    )
    private String password;
}
