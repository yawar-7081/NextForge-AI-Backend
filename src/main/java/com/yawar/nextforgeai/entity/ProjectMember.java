package com.yawar.nextforgeai.entity;

import com.yawar.nextforgeai.entity.enums.ProjectMemberRole;
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
@Table(
        name = "project_member_tx"
)
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProjectMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    User user;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    ProjectMemberRole projectMemberRole;

    @CreationTimestamp
    Instant createdAt;

    @UpdateTimestamp
    Instant updatedAt;

    @Builder.Default
    boolean isDeleted = false;
}