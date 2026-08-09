package com.yawar.nextforgeai.service;

import com.yawar.nextforgeai.dto.ProjectFileContentResponse;
import com.yawar.nextforgeai.dto.ProjectFileResponse;
import org.springframework.core.io.Resource;

public interface ProjectFileService {
    ProjectFileResponse getProjectFilePaths(String projectId);

    ProjectFileContentResponse getProjectPathContent(String projectId, String path);

    void saveFile(String projectId, String path, String fileContent);

    Resource downloadProject(String projectId);
}
