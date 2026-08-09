package com.yawar.nextforgeai.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubscriptionResponse{
        PlanResponse plan;
        String status;
        Instant currentPeriodEnd;
        Long tokenUsedThisCycle;

}
