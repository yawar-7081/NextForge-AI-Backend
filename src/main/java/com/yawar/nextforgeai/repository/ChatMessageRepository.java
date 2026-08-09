package com.yawar.nextforgeai.repository;

import com.yawar.nextforgeai.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage,String> {

    @Query("""
        SELECT cm FROM ChatMessage cm
        JOIN ChatSession cs ON cs.id = cm.chatSession.id
        WHERE cs.project.id = :projectId
        AND cs.user.id = :userId
""")
    List<ChatMessage> findByProjectIdAndUserId(
            @Param("projectId") String projectId,@Param("userId") String userId
    );
}
