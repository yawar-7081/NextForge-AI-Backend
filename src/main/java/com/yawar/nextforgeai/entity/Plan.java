package com.yawar.nextforgeai.entity;


import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
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
        name = "plan_tx"
)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(nullable = false, unique = true)
    String name;

    @Column(unique = true)
    String stripePriceId;

    @Builder.Default
    @Column(nullable = false)
    Integer maxProjects = 0;

    @Builder.Default
    @Column(nullable = false)
    Integer maxTokensPerDay = 0;

    @Builder.Default
    @Column(nullable = false)
    Integer maxPreviews = 0;

    @Builder.Default
    @Column(nullable = false)
    boolean unlimitedAi = false;

    @Builder.Default
    @Column(nullable = false)
    boolean active = true;

    @CreationTimestamp
    Instant createdAt;

    @UpdateTimestamp
    Instant updatedAt;
}