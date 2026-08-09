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
@Builder
@Table(
        name = "email_verification_token_tx",
        indexes = {
                @Index(name = "idx_evt_email_otp", columnList = "email, otp")
        }
)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(nullable = false, updatable = false)
    String email;

    @Column(nullable = false, updatable = false)
    String otp;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    Long expiresAt = System.currentTimeMillis() + 1000 * 60 * 5;

    @Builder.Default
    @Column(nullable = false)
    boolean isUsed = false;

    @CreationTimestamp
    Instant createdAt;

}