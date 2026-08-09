package com.yawar.nextforgeai.controller;

import com.yawar.nextforgeai.dto.ProjectFileContentResponse;
import com.yawar.nextforgeai.dto.ProjectFileResponse;
import com.yawar.nextforgeai.service.ProjectFileService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/project-file/{projectId}")
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
public class ProjectFileController {

    ProjectFileService projectFileService;

    @GetMapping
    public ResponseEntity<ProjectFileResponse> getProjectPaths(
            @PathVariable(value = "projectId", required = true) String projectId
    ){
        ProjectFileResponse responses = projectFileService.getProjectFilePaths(projectId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/content")
    public ResponseEntity<ProjectFileContentResponse> getProjectPathContent(
            @PathVariable(value = "projectId", required = true) String projectId,
            @RequestParam(value = "path",required = true) String path
    ){
        ProjectFileContentResponse responses = projectFileService.getProjectPathContent(projectId,path);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadProject(
            @PathVariable String projectId
    ) {

        Resource resource = projectFileService.downloadProject(projectId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"nextforge-project.zip\""
                )
                .body(resource);
    }
}
