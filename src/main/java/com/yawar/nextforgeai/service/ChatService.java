package com.yawar.nextforgeai.service;

import com.yawar.nextforgeai.dto.ChatResponse;

import java.util.List;

public interface ChatService {
    List<ChatResponse> getProjectChatHistory(String projectId);
}
