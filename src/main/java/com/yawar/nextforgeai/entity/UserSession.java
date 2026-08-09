package com.yawar.nextforgeai.entity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "user_session",
        indexes = {
                @Index(name = "idx_session_user", columnList = "user_id"),
                @Index(name = "idx_session_refresh_token", columnList = "refresh_token")
        }
)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    User user;

    @Column(nullable = false, unique = true, length = 512)
    String refreshToken;

    @Column(nullable = false)
    Instant expiresAt;

    @Builder.Default
    @Column(nullable = false)
    boolean revoked = false;

    String ipAddress;

    String userAgent;

    String deviceName;

    @CreationTimestamp
    Instant createdAt;

    @UpdateTimestamp
    Instant updatedAt;
}