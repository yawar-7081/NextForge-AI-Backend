package com.yawar.nextforgeai.service.impl;


import com.yawar.nextforgeai.dto.StreamResponse;
import com.yawar.nextforgeai.dto.TotalTokenResponse;
import com.yawar.nextforgeai.entity.*;
import com.yawar.nextforgeai.entity.enums.ChatEventType;
import com.yawar.nextforgeai.entity.enums.MessageRole;
import com.yawar.nextforgeai.error.BadRequestException;
import com.yawar.nextforgeai.error.ResourceNotFoundException;
import com.yawar.nextforgeai.llm.ChatEventParser;
import com.yawar.nextforgeai.llm.FileTreeContextAdvisor;
import com.yawar.nextforgeai.llm.CodeGenerationTools;
import com.yawar.nextforgeai.repository.*;
import com.yawar.nextforgeai.security.JwtService;
import com.yawar.nextforgeai.service.AiGenerationService;
import com.yawar.nextforgeai.service.ProjectFileService;
import com.yawar.nextforgeai.service.UsageService;
import com.yawar.nextforgeai.util.PromptUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiGenerationServiceImpl implements AiGenerationService {

    private final ChatEventRepository chatEventRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final JwtService jwtService;
    private final ChatClient chatClient;
    private final FileTreeContextAdvisor fileTreeContextAdvisor;
    private final ProjectFileService projectFileService;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ChatEventParser chatEventParser;
    private final UsageService usageService;
    private final UsageLogRepository usageLogRepository;

    private final Long totalToken = 20000L;
    @PreAuthorize(value = "@security.canEditProject(#projectId)")
    @Override
    public Flux<StreamResponse> streamResponse(String message, String projectId) {

        String userId = jwtService.getLoggedInUserId();

        TotalTokenResponse totalTokenResponse = usageService.getTotalToken();

        if(totalTokenResponse.getTotalToken() > totalToken){
            throw new BadRequestException("Daily Quota End.");
        }

        CodeGenerationTools codeGenerationTools = new CodeGenerationTools(projectFileService,projectId);

        ChatSession chatSession = createChatSessionIfNotExists(userId,projectId);

        Map<String,Object> advisorParams = Map.of(
                "projectId",projectId,
                "userId",userId
        );

        StringBuilder fullResponseBuffer = new StringBuilder();

        AtomicReference<Long> startTime = new AtomicReference<>(0L);
        AtomicReference<Long> endTime = new AtomicReference<>(0L);
        AtomicReference<Usage> usage = new AtomicReference<>();

        return chatClient.prompt()
                .system(PromptUtils.CODE_GENERATION_SYSTEM_PROMPT)
                .user(message)
                .advisors(spec -> {
                    spec.params(advisorParams);
                    spec.advisors(
                            fileTreeContextAdvisor);
                })
                .tools(codeGenerationTools)
                .stream()
                .chatResponse()
                .doOnNext(response -> {
                    String content = response.getResult().getOutput().getText();
                    fullResponseBuffer.append(content);

                    if(content != null && !content.isEmpty() && endTime.get()==0){
                        endTime.set(System.currentTimeMillis());
                    }

                    if(response.getMetadata().getUsage() != null){
                        usage.set(response.getMetadata().getUsage());
                    }

                })
                .doOnComplete(() -> {
                    Long duration = (endTime.get() - startTime.get()) / 1000;
                    finalizeChats(chatSession,fullResponseBuffer,message,projectId,duration,usage.get(),userId);
                })
                .doOnError(error -> {
                    log.error("Error while generating code - {}",error.toString());
                })
                .map(response -> {
                    String text = response.getResult().getOutput().getText();
                    return new StreamResponse(text != null ? text : "");
                });
    }

    private void finalizeChats(ChatSession chatSession, StringBuilder fullResponseBuffer, String userMessage,String projectId, Long duration,Usage usage,String userId) {

        if(usage != null){
            Long token = Long.valueOf(usage.getCompletionTokens());
            usageService.recordToken(token,userId);
        }

        ChatMessage userChatMessage = ChatMessage.builder()
                .chatSession(chatSession)
                .content(userMessage)
                .role(MessageRole.USER)
                .build();

        chatMessageRepository.save(userChatMessage);

        ChatMessage assistantMessage = ChatMessage.builder()
                .chatSession(chatSession)
                .content("Assistant Message")
                .role(MessageRole.ASSISTANT)
                .build();

        assistantMessage = chatMessageRepository.save(assistantMessage);

       List<ChatEvent> chatEvents = chatEventParser.parse(assistantMessage,fullResponseBuffer);

       chatEvents.addFirst(ChatEvent.builder()
                       .chatMessage(assistantMessage)
                       .chatEventType(ChatEventType.THOUGHT)
                       .content("Thought for - "+duration+"s")
                       .sequenceOrder(0)
               .build()
       );

       chatEvents.stream().filter(ce -> ce.getChatEventType()==ChatEventType.FILE_EDIT)
                       .forEach(ce -> projectFileService.saveFile(projectId,ce.getFilePath(),ce.getContent()));

       chatEventRepository.saveAll(chatEvents);
    }

    private ChatSession createChatSessionIfNotExists(String userId, String projectId) {

        ChatSession chatSession = chatSessionRepository.findByProjectIdAndUserIdAndIsDeletedFalse(projectId,userId)
                .orElse(null);

        if(chatSession == null){
            Project project = projectRepository.findAccessibleProject(projectId,userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Project",projectId));

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User",userId));

            chatSession = ChatSession
                    .builder()
                    .project(project)
                    .user(user)
                    .build();

            chatSession = chatSessionRepository.save(chatSession);
        }

        return chatSession;
    }
}














