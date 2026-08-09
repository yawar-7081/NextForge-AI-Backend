package com.yawar.nextforgeai.service.impl;

import com.yawar.nextforgeai.dto.TotalTokenResponse;
import com.yawar.nextforgeai.dto.UsedTokenResponse;
import com.yawar.nextforgeai.entity.UsageLog;
import com.yawar.nextforgeai.entity.User;
import com.yawar.nextforgeai.error.ResourceNotFoundException;
import com.yawar.nextforgeai.repository.UsageLogRepository;
import com.yawar.nextforgeai.repository.UserRepository;
import com.yawar.nextforgeai.security.JwtService;
import com.yawar.nextforgeai.service.UsageService;
import com.yawar.nextforgeai.util.CacheNames;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@RequiredArgsConstructor
@Service
@Slf4j
public class UsageServiceImpl implements UsageService {

    private final UsageLogRepository usageLogRepository;
    private final JwtService jwtService;
    private final UserRepository userRepository;


    @Override
    @Cacheable(
            value = CacheNames.USER_TOKEN_USAGE,
            key = "#root.target.jwtService.getLoggedInUserId()",
            unless = "#result == null"
    )
    public TotalTokenResponse getTotalToken() {

        String userId = jwtService.getLoggedInUserId();

        log.info("Fetching today's token usage. userId={}", userId);

        UsageLog usageLog = usageLogRepository
                .findByUserIdAndDate(userId, LocalDate.now())
                .orElse(null);

        long totalUsedTokens = usageLog != null
                ? usageLog.getTotalUsedTokens()
                : 0L;

        log.info("Token usage fetched successfully. userId={}, tokensUsed={}",
                userId,
                totalUsedTokens);

        return new TotalTokenResponse(
                userId,
                totalUsedTokens
        );
    }

    @Override
    @Cacheable(
            value = CacheNames.USER_TOKEN_USAGE,
            key = "#root.target.jwtService.getLoggedInUserId()",
            unless = "#result == null"
    )
    public UsedTokenResponse getUsedToken() {

        String userId = jwtService.getLoggedInUserId();

        log.info("Fetching used tokens. userId={}", userId);

        UsageLog usageLog = usageLogRepository
                .findByUserIdAndDate(userId, LocalDate.now())
                .orElse(null);

        long usedTokens = usageLog != null
                ? usageLog.getTotalUsedTokens()
                : 0L;

        log.info("Used tokens fetched successfully. userId={}, usedTokens={}",
                userId,
                usedTokens);

        return UsedTokenResponse.builder()
                .userId(userId)
                .usedToken(usedTokens)
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(
            value = CacheNames.USER_TOKEN_USAGE,
            key = "#userId"
    )
    public void recordToken(Long token, String userId) {

        log.info("Recording token usage. userId={}, tokens={}", userId, token);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        UsageLog usageLog = usageLogRepository.findByUserId(userId)
                .orElse(null);

        if (usageLog == null) {

            log.info("Creating new usage log. userId={}", userId);

            usageLog = UsageLog.builder()
                    .user(user)
                    .totalUsedTokens(0L)
                    .build();
        }

        usageLog.setTotalUsedTokens(
                usageLog.getTotalUsedTokens() + token
        );

        usageLogRepository.save(usageLog);

        log.info("Token usage updated successfully. userId={}, totalTokens={}",
                userId,
                usageLog.getTotalUsedTokens());
    }
}
