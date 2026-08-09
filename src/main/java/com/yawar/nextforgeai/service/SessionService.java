package com.yawar.nextforgeai.service;

import com.yawar.nextforgeai.entity.User;

public interface SessionService {

    String createSession(
            User user,
            String ipAddress,
            String userAgent,
            String deviceName
    );

    void revokeSession(String refreshToken);

    void revokeAllSessions(String userId);

}