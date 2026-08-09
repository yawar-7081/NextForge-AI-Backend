package com.yawar.nextforgeai.service.impl;

import com.yawar.nextforgeai.entity.Project;
import com.yawar.nextforgeai.entity.ProjectFile;
import com.yawar.nextforgeai.error.ResourceNotFoundException;
import com.yawar.nextforgeai.repository.ProjectFileRepository;
import com.yawar.nextforgeai.repository.ProjectRepository;
import com.yawar.nextforgeai.service.ProjectTemplateService;
import io.github.resilience4j.retry.annotation.Retry;
import io.minio.*;
import io.minio.messages.Item;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectTemplateServiceImpl implements ProjectTemplateService {

    private final ProjectFileRepository projectFileRepository;
    private final ProjectRepository projectRepository;
    private final MinioClient minioClient;


    private static final String TEMPLATE_BUCKET = "starter-project";
    private static final String TARGET_BUCKET = "projects";
    private static final String TEMPLATE_NAME = "react-starter-template-project";
    @Override
    @Transactional
    @Retry(name = "storageRetry")
    public void initializeProjectTemplate(String projectId) {

        log.info("Initializing project template. projectId={}", projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        try {

            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(TEMPLATE_BUCKET)
                            .prefix(TEMPLATE_NAME + "/")
                            .recursive(true)
                            .build()
            );

            List<ProjectFile> projectFiles = new ArrayList<>();

            for (Result<Item> result : results) {

                Item item = result.get();

                String sourceKey = item.objectName();

                String cleanPath = sourceKey.replace(TEMPLATE_NAME + "/", "");

                String destinationKey = projectId + "/" + cleanPath;

                log.debug("Copying template file. source={}, destination={}",
                        sourceKey,
                        destinationKey);

                minioClient.copyObject(
                        CopyObjectArgs.builder()
                                .bucket(TARGET_BUCKET)
                                .object(destinationKey)
                                .source(
                                        CopySource.builder()
                                                .bucket(TEMPLATE_BUCKET)
                                                .object(sourceKey)
                                                .build()
                                )
                                .build()
                );

                projectFiles.add(
                        ProjectFile.builder()
                                .project(project)
                                .path(cleanPath)
                                .minioObjectKey(destinationKey)
                                .build()
                );
            }

            projectFileRepository.saveAll(projectFiles);

            log.info("Project template initialized successfully. projectId={}, totalFiles={}",
                    projectId,
                    projectFiles.size());

        } catch (Exception ex) {

            log.error("Failed to initialize project template. projectId={}",
                    projectId,
                    ex);

            throw new RuntimeException("Failed to initialize project template.", ex);
        }
    }
}
