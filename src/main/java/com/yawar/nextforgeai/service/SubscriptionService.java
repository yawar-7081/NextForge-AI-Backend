package com.yawar.nextforgeai.service;

import com.yawar.nextforgeai.dto.SubscriptionResponse;
import com.yawar.nextforgeai.entity.enums.SubscriptionStatus;

import java.time.Instant;

public interface SubscriptionService {
    SubscriptionResponse getCurrentSubscription();

    void activateSubscription(String userId, String planId, String subscriptionId, String customerId);

    void updateSubscription(String gatewaySubscriptionId, SubscriptionStatus subscriptionStatus, Instant periodStart, Instant periodEnd, Boolean cancelAtPeriodEnd, String planId);

    void cancelSubscription(String gatewaySubscriptionId);

    void renewSubscriptionPeriod(String gatewaySubscriptionId, Instant periodStart, Instant periodEnd);

    void markSubscriptionPastDue(String subId);

    boolean canCreateNewProject();
}
