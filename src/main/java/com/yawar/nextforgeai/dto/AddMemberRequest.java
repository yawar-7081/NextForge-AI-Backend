package com.yawar.nextforgeai.dto;

import com.yawar.nextforgeai.entity.enums.ProjectMemberRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Data;


@Data
public class AddMemberRequest {
    @NotBlank(message = "'username' cannot be blank or empty")
    private String username;

    @NotNull(message = "'role' is required")
    private ProjectMemberRole role;
}
