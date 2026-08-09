package com.yawar.nextforgeai.llm;

import com.yawar.nextforgeai.dto.ProjectFileResponse;
import com.yawar.nextforgeai.service.ProjectFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class FileTreeContextAdvisor implements StreamAdvisor {

    private final ProjectFileService projectFileService;

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {

        Map<String,Object> context = chatClientRequest.context();

        String projectId = context.getOrDefault("projectId",0).toString();
        ChatClientRequest augmentedRequest = augmentRequestWithFileTree(projectId,chatClientRequest);
        return streamAdvisorChain.nextStream(augmentedRequest);
    }

    private ChatClientRequest augmentRequestWithFileTree(String projectId, ChatClientRequest chatClientRequest) {


        List<Message> incomingMessages = chatClientRequest.prompt().getInstructions();

        Message systemMessage = incomingMessages.stream().filter(message -> message.getMessageType()== MessageType.SYSTEM)
                .findFirst().orElse(null);

        List<Message> userMessages = incomingMessages.stream().filter(message -> message.getMessageType() != MessageType.SYSTEM)
                .toList();

        List<Message> allMessages = new ArrayList<>();

        if(systemMessage != null){
            allMessages.add(systemMessage);
        }

        ProjectFileResponse fileTree = projectFileService.getProjectFilePaths(projectId);

        String fileTreeContext = "\n\n ================================= FILE TREE ================================\n\n"+
                fileTree.getPaths().toString();


        allMessages.add(new SystemMessage(fileTreeContext));

        allMessages.addAll(userMessages);

        return chatClientRequest.mutate()
                .prompt(new Prompt(allMessages,chatClientRequest.prompt().getOptions()))
                .build();
    }

    @Override
    public String getName() {
        return "FileTreeContextAdvisor";
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
