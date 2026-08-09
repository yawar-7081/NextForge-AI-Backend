package com.yawar.nextforgeai.repository;

import com.yawar.nextforgeai.entity.Subscription;
import com.yawar.nextforgeai.entity.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.Optional;
import java.util.Set;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription,String> {
    //    Get the current active subscription
    Optional<Subscription> findByUserIdAndSubscriptionStatusIn(String userId, Set<SubscriptionStatus> active);

    boolean existsByStripeSubscriptionId(String subscriptionId);

    Optional<Subscription> findByStripeSubscriptionId(String gatewaySubscriptionId);

    Optional<Subscription> findByUserId(String userId);
}
