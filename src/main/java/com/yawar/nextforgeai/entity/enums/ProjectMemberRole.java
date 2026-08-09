package com.yawar.nextforgeai.entity.enums;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;

import static com.yawar.nextforgeai.entity.enums.ProjectPermission.*;


@Getter
public enum ProjectMemberRole {
    OWNER(EDIT,VIEW,MANAGE_MEMBERS,VIEW_MEMBERS,DELETE),
    EDITOR(VIEW,EDIT,VIEW_MEMBERS,DELETE),
    VIEWER(VIEW,VIEW_MEMBERS);

    ProjectMemberRole(ProjectPermission... permission){
        this.permissions = Set.of(permission);
    }

    private final Set<ProjectPermission> permissions;
}
