package com.yawar.nextforgeai.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class AuthResponse {
    private String userId;
    private String name;
    private String email;
    private String username;
    private String accessToken;
    private String refreshToken;
}
