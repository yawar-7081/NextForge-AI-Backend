package com.yawar.nextforgeai.repository;

import com.yawar.nextforgeai.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken,String> {
    Optional<PasswordResetToken> findByToken(String token);
}
