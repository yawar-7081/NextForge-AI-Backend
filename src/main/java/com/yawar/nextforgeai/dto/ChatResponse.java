package com.yawar.nextforgeai.dto;

import com.yawar.nextforgeai.entity.enums.MessageRole;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class ChatResponse {
    private String id;
    private MessageRole role;
    private List<ChatEventResponse> events;
    private String content;
    private Long tokenUsed;
    private Instant createdAt;
}
