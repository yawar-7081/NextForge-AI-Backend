package com.yawar.nextforgeai.service.impl;

import com.yawar.nextforgeai.dto.PlanResponse;
import com.yawar.nextforgeai.dto.SubscriptionResponse;
import com.yawar.nextforgeai.entity.Plan;
import com.yawar.nextforgeai.entity.Subscription;
import com.yawar.nextforgeai.entity.UsageLog;
import com.yawar.nextforgeai.entity.User;
import com.yawar.nextforgeai.entity.enums.SubscriptionStatus;
import com.yawar.nextforgeai.error.ResourceNotFoundException;
import com.yawar.nextforgeai.repository.*;
import com.yawar.nextforgeai.security.JwtService;
import com.yawar.nextforgeai.service.SubscriptionService;
import com.yawar.nextforgeai.util.CacheNames;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final JwtService jwtService;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final Integer FREE_TIER_PROJECTS_ALLOWED = 100;
    private final UsageLogRepository usageLogRepository;
    @Override
    @Cacheable(
            value = CacheNames.USER_SUBSCRIPTION,
            key = "@jwtService.getLoggedInUserId()",
            unless = "#result == null"
    )
    public SubscriptionResponse getCurrentSubscription() {

        String userId = jwtService.getLoggedInUserId();

        log.info("Fetching current subscription. userId={}", userId);

        Subscription subscription = subscriptionRepository
                .findByUserIdAndSubscriptionStatusIn(
                        userId,
                        Set.of(
                                SubscriptionStatus.ACTIVE,
                                SubscriptionStatus.PAST_DUE,
                                SubscriptionStatus.TRAILING
                        )
                )
                .orElseThrow(() -> {
                    log.error("No active subscription found. userId={}", userId);
                    return new ResourceNotFoundException("Subscription", userId);
                });

        UsageLog usageLog = usageLogRepository
                .findByUserIdAndDate(userId, LocalDate.now())
                .orElse(null);

        Plan plan = subscription.getPlan();

        PlanResponse planResponse = PlanResponse.builder()
                .id(plan.getId())
                .name(plan.getName())
                .maxProjects(plan.getMaxProjects())
                .maxTokenPerDay(plan.getMaxTokensPerDay())
                .unlimitedAi(plan.isUnlimitedAi())
                .price(null) // TODO: Populate later from Stripe or add price column in Plan
                .build();

        log.info("Subscription fetched successfully. userId={}, plan={}",
                userId,
                plan.getName());

        return SubscriptionResponse.builder()
                .plan(planResponse)
                .status(subscription.getSubscriptionStatus().name())
                .currentPeriodEnd(subscription.getCurrentPeriodEnd())
                .tokenUsedThisCycle(
                        usageLog != null
                                ? usageLog.getTotalUsedTokens()
                                : 0L
                )
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(
            value = CacheNames.USER_SUBSCRIPTION,
            key = "#userId"
    )
    public void activateSubscription(
            String userId,
            String planId,
            String subscriptionId,
            String customerId
    ) {

        log.info("Activating subscription. userId={}, planId={}", userId, planId);

        Subscription subscription = subscriptionRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Subscription", userId));

        Plan plan = getPlan(planId);

        subscription.setPlan(plan);
        subscription.setStripeSubscriptionId(subscriptionId);
        subscription.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        subscription.setCancelAtPeriodEnd(false);

        subscriptionRepository.save(subscription);

        log.info("Subscription activated successfully. userId={}, plan={}",
                userId,
                plan.getName());
    }

    @Override
    public void updateSubscription(String gatewaySubscriptionId, SubscriptionStatus subscriptionStatus, Instant periodStart, Instant periodEnd, Boolean cancelAtPeriodEnd, String planId) {

        Subscription subscription = getSubscriptionByStripeSubscriptionId(gatewaySubscriptionId);

        boolean hasSubscriptionUpdated = false;

        if(subscriptionStatus != null && !subscription.getSubscriptionStatus().equals(subscriptionStatus)){
            subscription.setSubscriptionStatus(subscriptionStatus);
            hasSubscriptionUpdated = true;
        }

        if(periodStart != null && !subscription.getCurrentPeriodStart().equals(periodStart)){
            subscription.setCurrentPeriodStart(periodStart);
            hasSubscriptionUpdated = true;
        }

        if(periodEnd != null && !subscription.getCurrentPeriodEnd().equals(periodEnd)){
            subscription.setCurrentPeriodEnd(periodEnd);
            hasSubscriptionUpdated = true;
        }

        if(cancelAtPeriodEnd != null && cancelAtPeriodEnd != subscription.isCancelAtPeriodEnd()){
            subscription.setCancelAtPeriodEnd(cancelAtPeriodEnd);
            hasSubscriptionUpdated = true;
        }

        if(planId != null && !subscription.getPlan().getId().equals(planId)){
            Plan newPlan = getPlan(planId);
            subscription.setPlan(newPlan);
            hasSubscriptionUpdated = true;
        }

        if(hasSubscriptionUpdated){
            log.debug("Subscription has been updated: {}",gatewaySubscriptionId);
            subscriptionRepository.save(subscription);
        }

    }

    @Override
    public void cancelSubscription(String gatewaySubscriptionId) {

        Subscription subscription = getSubscriptionByStripeSubscriptionId(gatewaySubscriptionId);

        subscription.setSubscriptionStatus(SubscriptionStatus.CANCELLED);

        subscriptionRepository.save(subscription);

    }

    @Override
    public void renewSubscriptionPeriod(String gatewaySubscriptionId, Instant periodStart, Instant periodEnd) {
        Subscription subscription = getSubscriptionByStripeSubscriptionId(gatewaySubscriptionId);

        Instant newStart = periodStart != null ? periodStart : subscription.getCurrentPeriodStart();
        subscription.setCurrentPeriodStart(newStart);
        subscription.setCurrentPeriodEnd(periodEnd);

        if(subscription.getSubscriptionStatus() == SubscriptionStatus.PAST_DUE || subscription.getSubscriptionStatus() == SubscriptionStatus.INCOMPLETE){
            subscription.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        }

        subscriptionRepository.save(subscription);

    }

    @Override
    public void markSubscriptionPastDue(String gatewaySubscriptionId) {
        Subscription subscription = getSubscriptionByStripeSubscriptionId(gatewaySubscriptionId);

        if(subscription.getSubscriptionStatus() == SubscriptionStatus.PAST_DUE){
            log.debug("Subscription is already past due : {}",gatewaySubscriptionId);
            return;
        }

        subscription.setSubscriptionStatus(SubscriptionStatus.PAST_DUE);

        subscriptionRepository.save(subscription);

        // Notify user via email....
    }


    @Override
    public boolean canCreateNewProject() {
        String userId = jwtService.getLoggedInUserId();
        SubscriptionResponse currentSubscription = getCurrentSubscription();

        int count = projectMemberRepository.countProjectOwnedByUser(userId);

        if(currentSubscription.getPlan().getName().equals("FREE")){
            return count < FREE_TIER_PROJECTS_ALLOWED;
        }

        return count < currentSubscription.getPlan().getMaxProjects();
    }

    //    *****************Utility Methods****************
    private User getUser(String userId){
        return userRepository.findById(userId).orElseThrow(
                () -> new ResourceNotFoundException("User",userId.toString())
        );
    }
    
    private Plan getPlan(String planId){
        return planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan",planId.toString()));
    }
    
    private Subscription getSubscriptionByStripeSubscriptionId(String gatewaySubscriptionId){
        return subscriptionRepository
                .findByStripeSubscriptionId(gatewaySubscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription",gatewaySubscriptionId));
    }
    
}
