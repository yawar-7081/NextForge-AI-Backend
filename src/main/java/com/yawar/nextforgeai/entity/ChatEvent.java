package com.yawar.nextforgeai.entity;

import com.yawar.nextforgeai.entity.enums.ChatEventType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "chat_event_tx",
        indexes = {
                @Index(name = "idx_chatevent_msg_seq", columnList = "chat_message_id, sequenceOrder")
        }
)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_message_id", nullable = false, updatable = false)
    ChatMessage chatMessage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    ChatEventType chatEventType;

    @Column(nullable = false)
    Integer sequenceOrder;

    @Column(columnDefinition = "TEXT")
    String content;

    String filePath;

    @Column(columnDefinition = "TEXT")
    String metadata;

    @CreationTimestamp
    Instant createdAt;
}