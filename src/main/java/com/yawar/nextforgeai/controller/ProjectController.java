package com.yawar.nextforgeai.controller;

import com.yawar.nextforgeai.dto.ProjectRequest;
import com.yawar.nextforgeai.dto.ProjectResponse;
import com.yawar.nextforgeai.dto.ProjectSummaryResponse;
import com.yawar.nextforgeai.dto.WorkspaceStatusResponse;
import com.yawar.nextforgeai.service.ProjectService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/project")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
@Validated
public class ProjectController {

    ProjectService projectService;

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody(required = true) ProjectRequest projectRequest){
        ProjectResponse response = projectService.createProject(projectRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping(
            value = "/{projectId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ProjectSummaryResponse> getProjectById(
            @PathVariable(value = "projectId", required = true) String projectId
    ){
        ProjectSummaryResponse response = projectService.getProjectById(projectId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ProjectSummaryResponse>> getAllAccessibleProjects(){
        List<ProjectSummaryResponse> responses = projectService.getAllAccessibleProject();
        return ResponseEntity.ok(responses);
    }

    @PutMapping(
            value = "/{projectId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable(value = "projectId", required = true) String projectId,
            @Valid @RequestBody(required = true) ProjectRequest projectRequest
    ){
        ProjectResponse response = projectService.updateProject(projectId,projectRequest);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping(
            value = "/{projectId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Void> deleteProject(
            @PathVariable(value = "projectId", required = true) String projectId
    ){
        projectService.deleteProject(projectId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{projectId}/workspace-status")
    public ResponseEntity<WorkspaceStatusResponse> getWorkspaceStatus(
            @PathVariable String projectId
    ) {
        return ResponseEntity.ok(
                projectService.getWorkspaceStatus(projectId)
        );
    }

}
