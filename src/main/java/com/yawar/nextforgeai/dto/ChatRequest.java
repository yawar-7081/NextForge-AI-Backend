package com.yawar.nextforgeai.dto;

import lombok.Data;
import lombok.Getter;

@Data
public class ChatRequest {
    private String message;
    private String projectId;
}
