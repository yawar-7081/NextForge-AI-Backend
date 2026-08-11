package com.yawar.nextforgeai.service.impl;

import com.yawar.nextforgeai.dto.*;
import com.yawar.nextforgeai.entity.Project;
import com.yawar.nextforgeai.entity.ProjectMember;
import com.yawar.nextforgeai.entity.User;
import com.yawar.nextforgeai.entity.enums.ChatEventType;
import com.yawar.nextforgeai.entity.enums.ProjectMemberRole;
import com.yawar.nextforgeai.error.BadRequestException;
import com.yawar.nextforgeai.error.ResourceNotFoundException;
import com.yawar.nextforgeai.projection.ProjectWithRole;
import com.yawar.nextforgeai.repository.ChatEventRepository;
import com.yawar.nextforgeai.repository.ProjectMemberRepository;
import com.yawar.nextforgeai.repository.ProjectRepository;
import com.yawar.nextforgeai.repository.UserRepository;
import com.yawar.nextforgeai.security.JwtService;
import com.yawar.nextforgeai.service.EmailService;
import com.yawar.nextforgeai.service.ProjectService;
import com.yawar.nextforgeai.service.ProjectTemplateService;
import com.yawar.nextforgeai.util.CacheNames;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectTemplateService projectTemplateService;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final JwtService jwtService;
    private final ChatEventRepository chatEventRepository;
    private final EmailService emailService;

    @Override
    @Transactional
    @CacheEvict(
            value = CacheNames.USER_PROJECTS,
            key = "@jwtService.getLoggedInUserId()"
    )
    public ProjectResponse createProject(ProjectRequest projectRequest) {

        String userId = jwtService.getLoggedInUserId();

        if (projectRepository.existsByUserIdAndIsDeletedFalse(userId)) {
            throw new BadRequestException(
                    "You already have an active project."
            );
        }

        Optional<Project> lastDeletedProject =
                projectRepository
                        .findTopByUserIdAndIsDeletedTrueOrderByDeletedAtDesc(userId);

        if (lastDeletedProject.isPresent()) {

            Instant availableAt = lastDeletedProject
                    .get()
                    .getDeletedAt()
                    .plus(24, ChronoUnit.HOURS);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

            if (Instant.now().isBefore(availableAt)) {
                throw new BadRequestException(
                        "You can create a new project after " + formatter.format(availableAt)
                );
            }
        }

        log.info("Project creation requested. userId={}, projectName={}",
                userId,
                projectRequest.getProjectName());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Project project = Project.builder()
                .user(user)
                .name(projectRequest.getProjectName())
                .build();

        project = projectRepository.save(project);

        log.info("Project created successfully. projectId={}, ownerId={}",
                project.getId(),
                userId);

        ProjectMember ownerMember = ProjectMember.builder()
                .project(project)
                .user(user)
                .projectMemberRole(ProjectMemberRole.OWNER)
                .build();

        projectMemberRepository.save(ownerMember);

        log.info("Owner membership created. projectId={}, userId={}",
                project.getId(),
                userId);

        log.info("Initializing default project template. projectId={}",
                project.getId());

        projectTemplateService.initializeProjectTemplate(project.getId());

        log.info("Project template initialized successfully. projectId={}",
                project.getId());

        emailService.sendMailToOwner(
                "New Project Created",
                "PROJECT_CREATED",
                "A user has successfully created a new project on NextForge AI.",
                user.getName(),
                user.getUsername(),
                user.getEmail(),
                user.getId().toString(),
                "Project ID: " + project.getId()
        );
        return modelMapper.map(project, ProjectResponse.class);
    }

    @Override
    @Cacheable(
            value = CacheNames.PROJECT_DETAILS,
            key = "#projectId + ':' + @jwtService.getLoggedInUserId()",
            unless = "#result == null"
    )
    @PreAuthorize(value = "@security.canViewProject(#projectId)")
    public ProjectSummaryResponse getProjectById(String projectId) {

        String userId = jwtService.getLoggedInUserId();

        log.info("Fetching project details. projectId={}, userId={}", projectId, userId);

        ProjectWithRole project = projectRepository.getInMemberProject(projectId, userId)
                .orElseThrow(() -> {
                    log.warn("Project not found or access denied. projectId={}, userId={}",
                            projectId,
                            userId);

                    return new ResourceNotFoundException("Project", projectId);
                });

        log.info("Project details fetched successfully. projectId={}", projectId);

        return new ProjectSummaryResponse(
                project.getProject().getId(),
                project.getProject().getName(),
                project.getRole(),
                project.getProject().getCreatedAt(),
                project.getProject().getUpdatedAt()
        );
    }

    @Transactional
    @Override
    @Caching(evict = {
            @CacheEvict(value = CacheNames.PROJECT_DETAILS, key = "#projectId"),
            @CacheEvict(value = CacheNames.USER_PROJECTS, allEntries = true)
    })
    @PreAuthorize(value = "@security.canEditProject(#projectId)")
    public ProjectResponse updateProject(String projectId, ProjectRequest projectRequest) {

        String userId = jwtService.getLoggedInUserId();

        log.info("Project update requested. projectId={}, userId={}",
                projectId,
                userId);

        Project project = projectRepository.findAccessibleProject(projectId, userId)
                .orElseThrow(() -> {
                    log.warn("Project not found or access denied. projectId={}, userId={}",
                            projectId,
                            userId);

                    return new ResourceNotFoundException("Project", projectId);
                });

        project.setName(projectRequest.getProjectName());

        project = projectRepository.save(project);

        log.info("Project updated successfully. projectId={}, newName={}",
                project.getId(),
                project.getName());

        return modelMapper.map(project, ProjectResponse.class);
    }

    @Transactional
    @Override
    @Caching(evict = {
            @CacheEvict(value = CacheNames.PROJECT_DETAILS, key = "#projectId"),
            @CacheEvict(value = CacheNames.USER_PROJECTS, allEntries = true),
            @CacheEvict(value = CacheNames.PROJECT_MEMBERS, key = "#projectId"),
            @CacheEvict(value = CacheNames.PROJECT_FILE_TREE, key = "#projectId")
    })
    @PreAuthorize(value = "@security.canDeleteProject(#projectId)")
    public void deleteProject(String projectId) {

        String userId = jwtService.getLoggedInUserId();

        log.info("Project deletion requested. projectId={}, userId={}",
                projectId,
                userId);

        Project project = projectRepository.findAccessibleProject(projectId, userId)
                .orElseThrow(() -> {
                    log.warn("Project not found or access denied. projectId={}, userId={}",
                            projectId,
                            userId);

                    return new ResourceNotFoundException("Project", projectId);
                });

        if (project.isDeleted()) {
            log.warn("Project already deleted. projectId={}", projectId);
            throw new BadRequestException("Project has already been deleted.");
        }

        project.setDeleted(true);
        project.setDeletedAt(Instant.now());
        projectRepository.save(project);

        log.info("Project deleted successfully. projectId={}", projectId);
    }

    @Override
    @Cacheable(
            value = CacheNames.USER_PROJECTS,
            key = "@jwtService.getLoggedInUserId()",
            unless = "#result == null || #result.isEmpty()"
    )
    public List<ProjectSummaryResponse> getAllAccessibleProject() {

        String userId = jwtService.getLoggedInUserId();

        log.info("Fetching all accessible projects. userId={}", userId);

        List<ProjectSummaryResponse> projects = projectRepository
                .findAllAccessibleProjects(userId)
                .stream()
                .map(project -> new ProjectSummaryResponse(
                        project.getProject().getId(),
                        project.getProject().getName(),
                        project.getRole(),
                        project.getProject().getCreatedAt(),
                        project.getProject().getUpdatedAt()
                ))
                .toList();

        log.info("Fetched {} accessible projects for userId={}",
                projects.size(),
                userId);

        return projects;
    }

    @PreAuthorize("@security.canViewProject(#projectId)")
    @Override
    public WorkspaceStatusResponse getWorkspaceStatus(String projectId) {

        boolean initialized = chatEventRepository
                .existsByProjectIdAndChatEventType(
                        projectId,
                        ChatEventType.FILE_EDIT
                );

        return WorkspaceStatusResponse.builder()
                .initialized(initialized)
                .hasFileExplorer(initialized)
                .hasPreview(initialized)
                .canDownload(initialized)
                .build();
    }

}
