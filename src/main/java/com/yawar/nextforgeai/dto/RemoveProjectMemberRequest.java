package com.yawar.nextforgeai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RemoveProjectMemberRequest {
    @NotNull(message = "'projectMemberId' is required")
    private String projectMemberId;
}
