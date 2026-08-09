package com.yawar.nextforgeai.service;

import com.yawar.nextforgeai.dto.TotalTokenResponse;
import com.yawar.nextforgeai.dto.UsedTokenResponse;

public interface UsageService {
    TotalTokenResponse getTotalToken();

    UsedTokenResponse getUsedToken();

    void recordToken(Long token,String userId);
}
