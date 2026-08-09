package com.yawar.nextforgeai.projection;

import com.yawar.nextforgeai.entity.Project;
import com.yawar.nextforgeai.entity.enums.ProjectMemberRole;

public interface ProjectWithRole {
    Project getProject();
    ProjectMemberRole getRole();
}
