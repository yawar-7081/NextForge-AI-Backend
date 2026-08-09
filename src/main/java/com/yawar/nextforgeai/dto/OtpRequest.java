package com.yawar.nextforgeai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OtpRequest {
    @NotBlank(message = "'otp' can't be Empty or Blank !")
    @Size(min = 6,max = 6, message = "'otp' length should be exactly 6 characters")
    private String otp;
}
