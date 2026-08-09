package com.yawar.nextforgeai.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
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
@Table(
        name = "password_reset_token_tx"
)
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    User user;

    @Column(nullable = false, unique = true)
    String token;

    @Builder.Default
    Long expiresAt = System.currentTimeMillis() + 1000 * 60 * 10;

    @Builder.Default
    boolean isUsed = false;

    @CreationTimestamp
    Instant createdAt;
}