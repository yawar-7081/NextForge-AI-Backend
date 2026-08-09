package com.yawar.nextforgeai.entity;

import com.yawar.nextforgeai.entity.enums.Provider;
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
        name = "user_tx"
)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(nullable = false, updatable = false)
    String email;

    @Column(nullable = false,updatable = false,unique = true)
    String username;


    String name;

    String password;

    @Enumerated(EnumType.STRING)
    Provider provider;

    String providerUserId;

    @Builder.Default
    boolean isEmailVerified = false;

    String profilePicture;

    String stripeCustomerId;

    @CreationTimestamp
    Instant createdAt;

    @UpdateTimestamp
    Instant updatedAt;

    @Builder.Default
    boolean isDeleted = false;

    @Builder.Default
    boolean isActive = false;
}