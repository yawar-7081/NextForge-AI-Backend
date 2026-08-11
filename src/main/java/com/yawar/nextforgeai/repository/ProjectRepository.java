package com.yawar.nextforgeai.repository;

import com.yawar.nextforgeai.entity.Project;
import com.yawar.nextforgeai.entity.enums.ChatEventType;
import com.yawar.nextforgeai.projection.ProjectWithRole;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, String> {

    @Query("""
            SELECT p as project, pm.projectMemberRole as role 
            FROM Project p
            JOIN ProjectMember pm ON p.id = pm.project.id
            WHERE pm.project.id = :projectId
            AND pm.user.id = :userId
            AND p.isDeleted = false
            """)
    Optional<ProjectWithRole> getInMemberProject(@Param("projectId") String projectId, @Param("userId") String userId);


    @Query("""
            SELECT p FROM Project p
            WHERE p.id = :projectId
            AND p.isDeleted = false
            AND EXISTS(
                SELECT 1 FROM ProjectMember pm
                WHERE pm.user.id = :userId
                AND pm.project.id = :projectId
            )
            """)
    Optional<Project> findAccessibleProject(@Param("projectId") String projectId, @Param("userId") String userId);


    @Query("""
        SELECT p as project, pm.projectMemberRole as role
        FROM Project p
        JOIN ProjectMember pm ON p.id = pm.project.id
        WHERE pm.user.id = :userId
        AND p.isDeleted = false
        ORDER BY p.updatedAt DESC
""")
    List<ProjectWithRole> findAllAccessibleProjects(@Param("userId") String userId);

    @Query("""
        SELECT p FROM Project p
        JOIN ProjectMember pm ON p.id = pm.project.id
        WHERE pm.user.id = :userId
        AND p.id = :projectId
        AND pm.projectMemberRole = com.yawar.nextforgeai.entity.enums.ProjectMemberRole.OWNER
        AND p.isDeleted = false
    """)
    Optional<Project> getOwnerProject(@Param("projectId") String projectId,@Param("userId") String userId);


    // User cannot create another project while one is active
    boolean existsByUserIdAndIsDeletedFalse(String userId);

    // Get the most recently deleted project
    Optional<Project> findTopByUserIdAndIsDeletedTrueOrderByDeletedAtDesc(String userId);
}
