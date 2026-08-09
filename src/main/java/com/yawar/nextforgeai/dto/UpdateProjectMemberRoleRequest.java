package com.yawar.nextforgeai.dto;

import com.yawar.nextforgeai.entity.enums.ProjectMemberRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateProjectMemberRoleRequest {
    @NotNull(message = "'projectMemberId' is required")
    private String projectMemberId;
    @NotNull(message = "'role' is required")
    private ProjectMemberRole role;
}
