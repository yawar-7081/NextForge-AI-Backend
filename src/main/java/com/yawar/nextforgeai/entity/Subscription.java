package com.yawar.nextforgeai.entity;


import com.yawar.nextforgeai.entity.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
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
        name = "subscription_tx",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"plan_id", "user_id"})
        }
)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    SubscriptionStatus subscriptionStatus;

    String stripeSubscriptionId;

    Instant currentPeriodStart;

    Instant currentPeriodEnd;

    @Builder.Default
    @Column(nullable = false)
    boolean cancelAtPeriodEnd = false;

    @CreationTimestamp
    Instant createdAt;

    @UpdateTimestamp
    Instant updatedAt;
}