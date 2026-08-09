package com.yawar.nextforgeai.service;

import com.yawar.nextforgeai.dto.ProjectRequest;
import com.yawar.nextforgeai.dto.ProjectResponse;
import com.yawar.nextforgeai.dto.ProjectSummaryResponse;
import com.yawar.nextforgeai.dto.WorkspaceStatusResponse;
import jakarta.validation.Valid;

import java.util.List;

public interface ProjectService {
    ProjectResponse createProject(ProjectRequest projectRequest);

    ProjectSummaryResponse getProjectById(String projectId);

    ProjectResponse updateProject(String projectId, @Valid ProjectRequest projectRequest);

    void deleteProject(String projectId);

    List<ProjectSummaryResponse> getAllAccessibleProject();

    WorkspaceStatusResponse getWorkspaceStatus(String projectId);
}
