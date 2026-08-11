package com.yawar.nextforgeai.service.impl;

import com.yawar.nextforgeai.config.BackblazeB2Properties;
import com.yawar.nextforgeai.entity.Project;
import com.yawar.nextforgeai.entity.ProjectFile;
import com.yawar.nextforgeai.error.ResourceNotFoundException;
import com.yawar.nextforgeai.repository.ProjectFileRepository;
import com.yawar.nextforgeai.repository.ProjectRepository;
import com.yawar.nextforgeai.service.ProjectTemplateService;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectTemplateServiceImpl implements ProjectTemplateService {

    private final ProjectFileRepository projectFileRepository;
    private final ProjectRepository projectRepository;
    private final S3Client s3Client;
    private final BackblazeB2Properties properties;

    private static final String TEMPLATE_NAME = "react-starter-template-project";

    @Async
    @Override
    @Transactional
    @Retry(name = "storageRetry")
    public void initializeProjectTemplate(String projectId) {

        log.info("Initializing project template. projectId={}", projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Project", projectId));

        String bucketName = properties.getBucket();

        String templatePrefix =
                "starter-project/" + TEMPLATE_NAME + "/";

        try {

            List<ProjectFile> projectFiles = new ArrayList<>();

            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(templatePrefix)
                    .build();

            ListObjectsV2Iterable results =
                    s3Client.listObjectsV2Paginator(listRequest);

            for (S3Object item : results.contents()) {

                String sourceKey = item.key();

                String cleanPath = sourceKey.replaceFirst(
                        "^" + Pattern.quote(templatePrefix),
                        ""
                );

                if (cleanPath.isBlank()) {
                    continue;
                }

                String destinationKey =
                        projectId + "/" + cleanPath;

                log.debug(
                        "Copying template file. source={}, destination={}",
                        sourceKey,
                        destinationKey
                );

                CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                        .copySource(bucketName + "/" + sourceKey)
                        .bucket(bucketName)
                        .key(destinationKey)
                        .build();

                s3Client.copyObject(copyRequest);

                projectFiles.add(
                        ProjectFile.builder()
                                .project(project)
                                .path(cleanPath)
                                .minioObjectKey(destinationKey)
                                .build()
                );
            }

            projectFileRepository.saveAll(projectFiles);

            log.info(
                    "Project template initialized successfully. projectId={}, totalFiles={}",
                    projectId,
                    projectFiles.size()
            );

        } catch (Exception ex) {

            log.error(
                    "Failed to initialize project template. projectId={}",
                    projectId,
                    ex
            );

            throw new RuntimeException(
                    "Failed to initialize project template.",
                    ex
            );
        }
    }
}
