package com.yawar.nextforgeai.repository;

import com.yawar.nextforgeai.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken,String> {
    Optional<EmailVerificationToken> findTopByEmailAndIsUsedFalseOrderByCreatedAtDesc(String email);
}
