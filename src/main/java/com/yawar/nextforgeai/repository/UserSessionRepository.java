package com.yawar.nextforgeai.repository;

import com.yawar.nextforgeai.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession,String> {

    Optional<UserSession> findByRefreshTokenAndRevokedFalse(String refreshToken);

    List<UserSession> findByUserIdAndRevokedFalse(String userId);

    Optional<UserSession> findByUserId(String userId);

    boolean existsByRefreshToken(String refreshToken);

}