package com.yawar.nextforgeai.llm;


import com.yawar.nextforgeai.entity.ChatEvent;
import com.yawar.nextforgeai.entity.ChatMessage;
import com.yawar.nextforgeai.entity.enums.ChatEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class ChatEventParser {

    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "<message\\s+phase=\"([^\"]+)\">(.*?)</message>" +
                    "|<tool\\s+args=\"([^\"]+)\">(.*?)</tool>" +
                    "|<file\\s+path=\"([^\"]+)\">(.*?)</file>",
            Pattern.DOTALL
    );

    public List<ChatEvent> parse(ChatMessage chatMessage, StringBuilder llmOutput) {

        List<ChatEvent> events = new ArrayList<>();

        Matcher matcher = TOKEN_PATTERN.matcher(llmOutput);

        int order = 1;

        while (matcher.find()) {

            // <message>
            if (matcher.group(1) != null) {

                events.add(ChatEvent.builder()
                        .chatMessage(chatMessage)
                        .chatEventType(ChatEventType.MESSAGE)
                        .sequenceOrder(order++)
                        .content(matcher.group(2).trim())
                        .metadata(matcher.group(1))
                        .build());

                continue;
            }

            // <tool>
            if (matcher.group(3) != null) {

                events.add(ChatEvent.builder()
                        .chatMessage(chatMessage)
                        .chatEventType(ChatEventType.TOOL_LOG)
                        .sequenceOrder(order++)
                        .content(matcher.group(4).trim())
                        .metadata(matcher.group(3))
                        .build());

                continue;
            }

            // <file>
            if (matcher.group(5) != null) {

                events.add(ChatEvent.builder()
                        .chatMessage(chatMessage)
                        .chatEventType(ChatEventType.FILE_EDIT)
                        .sequenceOrder(order++)
                        .filePath(matcher.group(5).trim())
                        .content(matcher.group(6))
                        .build());
            }
        }

        return events;
    }

}
