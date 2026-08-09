package com.yawar.nextforgeai.entity;


import com.yawar.nextforgeai.entity.enums.MessageRole;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.List;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "chat_message_tx",
        indexes = {
                // Highly recommended: Speeds up loading the chat history
                @Index(name = "idx_chatmsg_session", columnList = "chat_session_id")
        }
)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_session_id", updatable = false, nullable = false)
    ChatSession chatSession;

    @Builder.Default
    @OneToMany(mappedBy = "chatMessage", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("sequenceOrder ASC")
    List<ChatEvent> events = new ArrayList<>();

    @Builder.Default
    @Column(nullable = false)
    Long tokenUsed = 0L;

    @Column(nullable = false)
    String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    MessageRole role;

    @CreationTimestamp
    Instant createdAt;
}