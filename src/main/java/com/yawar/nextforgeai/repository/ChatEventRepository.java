package com.yawar.nextforgeai.repository;


import com.yawar.nextforgeai.entity.ChatEvent;
import com.yawar.nextforgeai.entity.enums.ChatEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatEventRepository extends JpaRepository<ChatEvent,String> {
    List<ChatEvent> findByChatMessageId(String id);
    @Query("""
    SELECT COUNT(ce) > 0
    FROM ChatEvent ce
    JOIN ce.chatMessage cm
    JOIN cm.chatSession cs
    WHERE cs.project.id = :projectId
      AND ce.chatEventType = :type
""")
    boolean existsByProjectIdAndChatEventType(
            @Param("projectId") String projectId,
            @Param("type") ChatEventType type
    );
}
