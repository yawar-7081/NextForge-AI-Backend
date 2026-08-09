package com.yawar.nextforgeai.security;

import com.yawar.nextforgeai.entity.enums.ProjectPermission;
import com.yawar.nextforgeai.repository.ProjectMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.stereotype.Component;

@Component("security")
@RequiredArgsConstructor
@Slf4j
public class SecurityExpressions {

    private final ProjectMemberRepository projectMemberRepository;
    private final JwtService authUtil;


    private boolean hasPermission(String projectId, ProjectPermission projectPermission) {
        String userId = authUtil.getLoggedInUserId();

        log.debug("Checking permission for userId={} on projectId={}, requiredPermission={}",
                userId, projectId, projectPermission);

        boolean hasAccess = projectMemberRepository
                .findRoleByProjectIdAndUserId(projectId, userId)
                .map(role -> {
                    boolean permitted = role.getPermissions().contains(projectPermission);
                    log.debug("User role for project found: role={}, hasPermission={}", role, permitted);
                    return permitted;
                })
                .orElseGet(() -> {
                    log.debug("No project membership or role found for userId={} on projectId={}", userId, projectId);
                    return false;
                });

        log.info("Permission check result for userId={} on projectId={}: {}",
                userId, projectId, hasAccess);

        return hasAccess;
    }

    public boolean canViewProject(String projectId) {
        return hasPermission(projectId,ProjectPermission.VIEW);
    }

    public boolean canEditProject(String projectId) {
        return hasPermission(projectId,ProjectPermission.EDIT);
    }

    public boolean canDeleteProject(String projectId){
        return hasPermission(projectId,ProjectPermission.DELETE);
    }

    public boolean canViewMembers(String projectId){
        return hasPermission(projectId,ProjectPermission.VIEW_MEMBERS);
    }

    public boolean canManageMembers(String projectId){
        return hasPermission(projectId, ProjectPermission.MANAGE_MEMBERS);
    }
}
