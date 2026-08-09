package com.yawar.nextforgeai.service.impl;

import com.yawar.nextforgeai.entity.User;
import com.yawar.nextforgeai.entity.UserSession;
import com.yawar.nextforgeai.repository.UserSessionRepository;
import com.yawar.nextforgeai.security.JwtService;
import com.yawar.nextforgeai.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionServiceImpl implements SessionService {

    private final UserSessionRepository sessionRepository;
    private final JwtService jwtService;

    @Override
    @Transactional
    public String createSession(
            User user,
            String ipAddress,
            String userAgent,
            String deviceName
    ) {

        String refreshToken = jwtService.generateRefreshToken(user);

        UserSession session = UserSession.builder()
                .user(user)
                .refreshToken(refreshToken)
                .expiresAt(
                        Instant.now().plus(30, ChronoUnit.DAYS)
                )
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .deviceName(deviceName)
                .build();

        sessionRepository.save(session);

        log.info("User session created. userId={}", user.getId());

        return refreshToken;
    }

    @Override
    @Transactional
    public void revokeSession(String refreshToken) {

        UserSession session = sessionRepository
                .findByRefreshTokenAndRevokedFalse(refreshToken)
                .orElse(null);

        if(session == null){
            return;
        }

        session.setRevoked(true);

        sessionRepository.save(session);

        log.info("Refresh token revoked.");
    }

    @Override
    @Transactional
    public void revokeAllSessions(String userId) {

        List<UserSession> sessions =
                sessionRepository.findByUserIdAndRevokedFalse(userId);

        sessions.forEach(session -> session.setRevoked(true));

        sessionRepository.saveAll(sessions);

        log.info("All sessions revoked. userId={}", userId);
    }
}