package com.yawar.nextforgeai.service.impl;

import com.yawar.nextforgeai.dto.AddMemberRequest;
import com.yawar.nextforgeai.dto.MemberResponse;
import com.yawar.nextforgeai.dto.RemoveProjectMemberRequest;
import com.yawar.nextforgeai.dto.UpdateProjectMemberRoleRequest;
import com.yawar.nextforgeai.entity.Project;
import com.yawar.nextforgeai.entity.ProjectMember;
import com.yawar.nextforgeai.entity.User;
import com.yawar.nextforgeai.error.BadRequestException;
import com.yawar.nextforgeai.error.ResourceNotFoundException;
import com.yawar.nextforgeai.repository.ProjectMemberRepository;
import com.yawar.nextforgeai.repository.ProjectRepository;
import com.yawar.nextforgeai.repository.UserRepository;
import com.yawar.nextforgeai.security.JwtService;
import com.yawar.nextforgeai.service.ProjectMemberService;
import com.yawar.nextforgeai.util.CacheNames;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;


@RequiredArgsConstructor
@Service
@Slf4j
public class ProjectMemberServiceImpl implements ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;


    @Override
    @Transactional
    @PreAuthorize(value = "@security.canManageMembers(#projectId)")
    @CacheEvict(
            value = CacheNames.PROJECT_MEMBERS,
            key = "#projectId"
    )
    public MemberResponse addMember(String projectId, AddMemberRequest request) {

        String userId = jwtService.getLoggedInUserId();

        log.info("Add member request received. projectId={}, requestedBy={}, username={}",
                projectId,
                userId,
                request.getUsername());

        Project project = getProject(projectId, userId);

        User invitee = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getUsername()));

        if (project.getUser().getUsername().equals(invitee.getUsername())) {
            log.warn("Owner attempted to add themselves as a member. projectId={}, userId={}",
                    projectId,
                    userId);

            throw new BadRequestException("You cannot add yourself to your own project.");
        }

        ProjectMember projectMember = projectMemberRepository
                .findProjectMemberByProjectIdAndUserId(project.getId(), invitee.getId())
                .orElse(null);

        if (projectMember != null) {

            if (!projectMember.isDeleted()) {

                log.warn("User is already a project member. projectId={}, username={}",
                        projectId,
                        invitee.getUsername());

                throw new BadRequestException(
                        "User is already a member of this project."
                );
            }

            log.info("Restoring previously deleted project member. memberId={}",
                    projectMember.getId());

            projectMember.setDeleted(false);
            projectMember.setProjectMemberRole(request.getRole());

        } else {

            log.info("Creating new project member. projectId={}, username={}",
                    projectId,
                    invitee.getUsername());

            projectMember = ProjectMember.builder()
                    .project(project)
                    .user(invitee)
                    .projectMemberRole(request.getRole())
                    .isDeleted(false)
                    .build();
        }

        projectMember = projectMemberRepository.save(projectMember);

        log.info("Project member added successfully. projectId={}, memberId={}",
                projectId,
                projectMember.getId());

        return MemberResponse.builder()
                .id(projectMember.getId())
                .projectMemberRole(projectMember.getProjectMemberRole())
                .email(projectMember.getUser().getEmail())
                .username(projectMember.getUser().getUsername())
                .createdAt(projectMember.getCreatedAt())
                .build();
    }

    @Override
    @Cacheable(
            value = "project-members",
            key = "#projectId",
            unless = "#result == null || #result.isEmpty()"
    )
    public List<MemberResponse> getProjectMember(String projectId) {

        log.info("Fetching project members. projectId={}", projectId);

        List<ProjectMember> pms = projectMemberRepository.getProjectMembers(projectId);

        List<MemberResponse> members = pms
                .stream()
                .map(pm -> MemberResponse.builder()
                        .id(pm.getId())
                        .projectMemberRole(pm.getProjectMemberRole())
                        .email(pm.getUser().getEmail())
                        .username(pm.getUser().getUsername())
                        .createdAt(pm.getCreatedAt())
                        .build())
                .toList();

        log.info("Project members fetched successfully. projectId={}, totalMembers={}",
                projectId,
                members.size());

        return members;
    }

    @Transactional
    @Override
    @CacheEvict(
            value = CacheNames.PROJECT_MEMBERS,
            key = "#projectId"
    )
    @PreAuthorize(value = "@security.canManageMembers(#projectId)")
    public MemberResponse updateMemberRole(String projectId, UpdateProjectMemberRoleRequest request) {

        String userId = jwtService.getLoggedInUserId();

        log.info("Updating member role. projectId={}, memberId={}, requestedBy={}",
                projectId,
                request.getProjectMemberId(),
                userId);

        Project project = getProject(projectId, userId);

        ProjectMember projectMember = projectMemberRepository
                .findByProjectIdAndMemberId(project.getId(), request.getProjectMemberId())
                .orElseThrow(() -> {
                    log.warn("Invalid project member. projectId={}, memberId={}",
                            projectId,
                            request.getProjectMemberId());

                    return new BadRequestException(
                            "Invalid Project Member Id - " + request.getProjectMemberId()
                    );
                });

        if (projectMember.getProjectMemberRole() == request.getRole()) {

            log.warn("Member already has role {}. memberId={}",
                    request.getRole(),
                    projectMember.getId());

            throw new BadRequestException(
                    "Member already has role " + request.getRole()
            );
        }

        projectMember.setProjectMemberRole(request.getRole());

        projectMember = projectMemberRepository.save(projectMember);

        log.info("Member role updated successfully. memberId={}, newRole={}",
                projectMember.getId(),
                projectMember.getProjectMemberRole());

        return MemberResponse.builder()
                .id(projectMember.getId())
                .projectMemberRole(projectMember.getProjectMemberRole())
                .email(projectMember.getUser().getEmail())
                .username(projectMember.getUser().getUsername())
                .createdAt(projectMember.getCreatedAt())
                .build();
    }

    @Transactional
    @Override
    @CacheEvict(
            value = CacheNames.PROJECT_MEMBERS,
            key = "#projectId"
    )
    @PreAuthorize(value = "@security.canManageMembers(#projectId)")
    public void removeMember(String projectId, RemoveProjectMemberRequest request) {

        String userId = jwtService.getLoggedInUserId();

        log.info("Remove member request received. projectId={}, memberId={}, requestedBy={}",
                projectId,
                request.getProjectMemberId(),
                userId);

        Project project = getProject(projectId, userId);

        ProjectMember projectMember = projectMemberRepository
                .findByProjectIdAndMemberId(project.getId(), request.getProjectMemberId())
                .orElseThrow(() -> {
                    log.warn("Invalid project member. projectId={}, memberId={}",
                            projectId,
                            request.getProjectMemberId());

                    return new BadRequestException(
                            "Invalid Project Member Id - " + request.getProjectMemberId()
                    );
                });

        if (projectMember.isDeleted()) {
            log.warn("Project member already removed. memberId={}", projectMember.getId());
            throw new BadRequestException("Project member has already been removed.");
        }

        projectMember.setDeleted(true);

        projectMemberRepository.save(projectMember);

        log.info("Project member removed successfully. memberId={}",
                projectMember.getId());
    }

    // INTERNAL METHOD
    private Project getProject(String projectId,String userId){
        return projectRepository.getOwnerProject(projectId,userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project",projectId));
    }

}
