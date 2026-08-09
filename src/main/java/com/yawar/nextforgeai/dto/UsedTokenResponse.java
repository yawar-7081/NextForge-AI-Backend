package com.yawar.nextforgeai.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UsedTokenResponse {
    private String userId;
    private Long usedToken;
}
