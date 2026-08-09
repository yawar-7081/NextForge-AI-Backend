package com.yawar.nextforgeai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CheckoutSessionUrlRequest {
    @NotBlank(message = "'planId' should not be blank")
    private String priceId;
}
