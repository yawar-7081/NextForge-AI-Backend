package com.yawar.nextforgeai.repository;

import com.yawar.nextforgeai.entity.UsageLog;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface UsageLogRepository extends JpaRepository<UsageLog,String> {
    Optional<UsageLog> findByUserId(String userId);

    Optional<UsageLog> findByUserIdAndDate(String userId, LocalDate now);
}
