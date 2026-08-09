package com.yawar.nextforgeai.service.impl;

import com.yawar.nextforgeai.dto.FileNode;
import com.yawar.nextforgeai.dto.ProjectFileContentResponse;
import com.yawar.nextforgeai.dto.ProjectFileResponse;
import com.yawar.nextforgeai.entity.Project;
import com.yawar.nextforgeai.entity.ProjectFile;
import com.yawar.nextforgeai.error.BadRequestException;
import com.yawar.nextforgeai.error.ResourceNotFoundException;
import com.yawar.nextforgeai.repository.ProjectFileRepository;
import com.yawar.nextforgeai.repository.ProjectRepository;
import com.yawar.nextforgeai.security.JwtService;
import com.yawar.nextforgeai.service.ProjectFileService;
import io.github.resilience4j.retry.annotation.Retry;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectFileServiceImpl implements ProjectFileService {

    private final ProjectRepository projectRepository;
    private final ProjectFileRepository projectFileRepository;
    private final MinioClient minioClient;
    private final JwtService jwtService;

    private static final String BUCKET_NAME = "projects";

    @Override
    @Cacheable(
            value = "project-file-tree",
            key = "#projectId",
            unless = "#result == null"
    )
    @PreAuthorize("@security.canViewProject(#projectId)")
    public ProjectFileResponse getProjectFilePaths(String projectId) {

        String userId = jwtService.getLoggedInUserId();

        log.info("Fetching project file tree. projectId={}, userId={}", projectId, userId);

        projectRepository.findAccessibleProject(projectId, userId)
                .orElseThrow(() -> {
                    log.warn("Unauthorized access attempt. projectId={}, userId={}", projectId, userId);
                    return new BadRequestException("You can't access this project's file tree.");
                });

        List<ProjectFile> projectFiles = projectFileRepository.findByProjectId(projectId);

        log.info("Project file tree loaded successfully. projectId={}, totalFiles={}",
                projectId,
                projectFiles.size());

        return new ProjectFileResponse(
                projectFiles.stream()
                        .map(file -> new FileNode(file.getPath()))
                        .toList()
        );
    }


    @Override
    @Cacheable(
            value = "project-file-content",
            key = "#projectId + ':' + #path",
            unless = "#result == null"
    )
    @PreAuthorize("@security.canViewProject(#projectId)")
    public ProjectFileContentResponse getProjectPathContent(String projectId, String path) {

        String userId = jwtService.getLoggedInUserId();

        log.info("Fetching project file content. projectId={}, path={}, userId={}",
                projectId,
                path,
                userId);

        projectRepository.findAccessibleProject(projectId, userId)
                .orElseThrow(() -> {
                    log.warn("Unauthorized file access. projectId={}, path={}, userId={}",
                            projectId,
                            path,
                            userId);

                    return new BadRequestException("You can't access this project.");
                });

        String objectName = projectId + "/" + path;

        try (InputStream inputStream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(BUCKET_NAME)
                        .object(objectName)
                        .build())) {

            String content = new String(
                    inputStream.readAllBytes(),
                    StandardCharsets.UTF_8
            );

            log.info("Project file loaded successfully. projectId={}, path={}",
                    projectId,
                    path);

            return ProjectFileContentResponse.builder()
                    .path(path)
                    .content(content)
                    .build();

        } catch (Exception ex) {

            log.error(
                    "Failed to read project file. projectId={}, path={}",
                    projectId,
                    path,
                    ex
            );

            throw new RuntimeException("Unable to fetch project file.", ex);
        }
    }

    @Override
    @Transactional
    @Retry(name = "storageRetry")
    @Caching(evict = {
            @CacheEvict(value = "project-file-tree", key = "#projectId"),
            @CacheEvict(value = "project-file-content", key = "#projectId + ':' + #path")
    })
    public void saveFile(String projectId, String path, String fileContent) {

        log.info("Saving project file. projectId={}, path={}", projectId, path);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        String cleanPath = path.startsWith("/") ? path.substring(1) : path;
        String objectKey = projectId + "/" + cleanPath;

        byte[] contentBytes = fileContent.getBytes(StandardCharsets.UTF_8);

        try (InputStream inputStream = new ByteArrayInputStream(contentBytes)) {

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(objectKey)
                            .stream(inputStream, contentBytes.length, -1)
                            .contentType(determineContentType(cleanPath))
                            .build()
            );

            ProjectFile file = projectFileRepository
                    .findByProjectIdAndPath(projectId, cleanPath)
                    .orElseGet(() -> ProjectFile.builder()
                            .project(project)
                            .path(cleanPath)
                            .minioObjectKey(objectKey)
                            .createdAt(Instant.now())
                            .build());

            file.setUpdatedAt(Instant.now());

            projectFileRepository.save(file);

            log.info("Project file saved successfully. projectId={}, path={}", projectId, cleanPath);

        } catch (Exception ex) {

            log.error(
                    "Failed to save project file. projectId={}, path={}",
                    projectId,
                    cleanPath,
                    ex
            );

            throw new RuntimeException("Unable to save project file.", ex);
        }
    }


    @PreAuthorize("@security.canViewProject(#projectId)")
    @Override
    public Resource downloadProject(String projectId) {

        String userId = jwtService.getLoggedInUserId();

        projectRepository.findAccessibleProject(projectId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Project", projectId));

        List<ProjectFile> files =
                projectFileRepository.findByProjectId(projectId);

        try (
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ZipOutputStream zos = new ZipOutputStream(baos)
        ) {

            for (ProjectFile file : files) {

                InputStream is = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(BUCKET_NAME)
                                .object(file.getMinioObjectKey())
                                .build()
                );

                zos.putNextEntry(new ZipEntry(file.getPath()));

                is.transferTo(zos);

                zos.closeEntry();

                is.close();
            }

            zos.finish();

            return new ByteArrayResource(baos.toByteArray());

        } catch (Exception e) {

            log.error("Failed to download project {}", projectId, e);

            throw new RuntimeException("Failed to create project zip.", e);
        }
    }

    private String determineContentType(String path){
        String type = URLConnection.guessContentTypeFromName(path);

        if(type != null) return type;
        if(path.endsWith(".jsx") || path.endsWith(".ts") || path.endsWith(".tsx")) return "text/javascript";
        if(path.endsWith(".json")) return "application/json";
        if(path.endsWith(".css")) return "text/css";

        return "text/plain";
    }
}
