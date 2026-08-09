package com.yawar.nextforgeai.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "usage_log_tx",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id","date"}),
        indexes = {
                @Index(name = "idx_usagelog_created_at", columnList = "createdAt")
        }
)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    LocalDate date = LocalDate.now();

    @Builder.Default
    @Column(nullable = false)
    Long totalUsedTokens = 0L;

    @CreationTimestamp
    Instant createdAt;
}