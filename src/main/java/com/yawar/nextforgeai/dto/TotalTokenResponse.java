package com.yawar.nextforgeai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
public class TotalTokenResponse implements Serializable {
    private String userId;
    private Long totalToken;
}
