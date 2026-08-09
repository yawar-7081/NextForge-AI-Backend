package com.yawar.nextforgeai.dto;

import com.yawar.nextforgeai.entity.enums.ProjectMemberRole;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProjectSummaryResponse implements Serializable {
    private String id;
    private String name;
    private ProjectMemberRole projectMemberRole;
    private Instant createdAt;
    private Instant updatedAt;
}
