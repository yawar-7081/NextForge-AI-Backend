package com.yawar.nextforgeai.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProjectPermission {
    EDIT("project:edit"),
    VIEW("project:view"),
    MANAGE_MEMBERS("project:manage_members"),
    VIEW_MEMBERS("project:view_members"),
    DELETE("project:delete");

    private final String value;
}
