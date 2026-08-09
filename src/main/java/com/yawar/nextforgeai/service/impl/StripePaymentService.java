package com.yawar.nextforgeai.service.impl;

import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.yawar.nextforgeai.dto.CheckoutSessionUrlRequest;
import com.yawar.nextforgeai.dto.CheckoutSessionUrlResponse;
import com.yawar.nextforgeai.dto.PortalResponse;
import com.yawar.nextforgeai.entity.Plan;
import com.yawar.nextforgeai.entity.User;
import com.yawar.nextforgeai.entity.enums.SubscriptionStatus;
import com.yawar.nextforgeai.error.BadRequestException;
import com.yawar.nextforgeai.error.ResourceNotFoundException;
import com.yawar.nextforgeai.repository.PlanRepository;
import com.yawar.nextforgeai.repository.SubscriptionRepository;
import com.yawar.nextforgeai.repository.UserRepository;
import com.yawar.nextforgeai.security.JwtService;
import com.yawar.nextforgeai.service.PaymentService;
import com.yawar.nextforgeai.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class StripePaymentService implements PaymentService {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;

    @Value("${frontend.url}")
    private String frontEndUrl;

    @Value("${stripe.api.secret}")
    private String stripeSecretKey;


    @Override
    public CheckoutSessionUrlResponse createCheckoutSessionUrl(CheckoutSessionUrlRequest checkoutSessionUrlRequest) {

        String userId = jwtService.getLoggedInUserId();

        Plan plan = planRepository.findById(checkoutSessionUrlRequest.getPriceId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan", checkoutSessionUrlRequest.getPriceId()));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User",userId));

        StripeClient stripeClient = new StripeClient(stripeSecretKey);

        var params = SessionCreateParams.builder()
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPrice(plan.getStripePriceId())
                                .setQuantity(1L)
                                .build()
                )
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setSubscriptionData(
                        new SessionCreateParams.SubscriptionData.Builder()
                                .setBillingMode(
                                        SessionCreateParams.SubscriptionData.BillingMode.builder()
                                                .setType(SessionCreateParams.SubscriptionData.BillingMode.Type.FLEXIBLE)
                                                .build()
                                )
                                .build()
                )
                .setSuccessUrl(frontEndUrl + "/success.html?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(frontEndUrl + "/cancel.html")
                .putMetadata("user_id", userId)
                .putMetadata("plan_id", plan.getId());

        try{
            String stripeCustomerId = user.getStripeCustomerId();

            if(stripeCustomerId == null || stripeCustomerId.isBlank()){
                params.setCustomerEmail(user.getEmail());
            }else{
                params.setCustomer(stripeCustomerId); //stripe customer id
            }

            Session session = stripeClient.v1().checkout().sessions().create(params.build()); //making api call to stripe
            return new CheckoutSessionUrlResponse(session.getUrl());
        } catch (RuntimeException | StripeException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public PortalResponse openCustomerPortal() {
        String userId = jwtService.getLoggedInUserId();

        User user =userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User",userId));

        String stripeCustomerId = user.getStripeCustomerId();

        if(stripeCustomerId == null || stripeCustomerId.isEmpty()){
            throw new BadRequestException("User does not have a Stripe Customer Id, UserId: "+userId);
        }

        try {
            var portalSession = com.stripe.model.billingportal.Session.create(
                    com.stripe.param.billingportal.SessionCreateParams.builder()
                            .setCustomer(stripeCustomerId)
                            .setReturnUrl(frontEndUrl)
                            .build()
            );

            return new PortalResponse(portalSession.getUrl());

        } catch (StripeException e) {
            throw new RuntimeException(e);
        }
    }



    @Override
    public void handleWebhookEvent(String type, StripeObject stripeObject, Map<String, String> metadata) {

        log.info("Handling stripe event: {}",type);
        switch(type){
            case "checkout.session.completed" -> handleCheckoutSessionCompleted((Session) stripeObject,metadata); // One time, on checkout completed
            case "customer.subscription.updated" -> handleCustomerSubscriptionUpdated((com.stripe.model.Subscription) stripeObject); // updated - when user cancels, upgrades subscriptions
            case "customer.subscription.deleted" -> handleCustomerSubscriptionDeleted((com.stripe.model.Subscription) stripeObject); // when subscription ends, revoke the access
            case "invoice.paid" -> handleInvoicePaid((Invoice) stripeObject); // when invoice is paid
            case "invoice.payment_failed" -> handleInvoicePaymentFailed((Invoice) stripeObject); // when invoice is not paid, park as PAST_DUE
            default -> log.debug("Incoming the event : {}",type);
        }
    }

    private void handleCheckoutSessionCompleted(Session session,Map<String,String> metadata) {

        if(session == null){
            log.error("Session object was null handleCheckoutSessionCompleted");
            return;
        }

        String userId = metadata.get("user_id");
        String planId = metadata.get("plan_id");

        String subscriptionId = session.getSubscription();
        String customerId = session.getCustomer();

        User user = getUser(userId);

        if(user.getStripeCustomerId()==null){
            user.setStripeCustomerId(customerId);
            userRepository.save(user);
        }

        subscriptionService.activateSubscription(userId,planId,subscriptionId,customerId);
    }

    private void handleCustomerSubscriptionUpdated(com.stripe.model.Subscription subscription) {
        if(subscription==null){
            log.error("Subscription object is null handleCustomerSubscriptionUpdated");
            return;
        }

        SubscriptionStatus status = mapStripeStatusToEnum(subscription.getStatus());
        if(status == null){
            log.warn("Unknown status {} for subscription {}",subscription.getStatus(),subscription.getId());
            return;
        }

        SubscriptionItem item = subscription.getItems().getData().get(0);
        Instant periodStart = toInstant(item.getCurrentPeriodStart());
        Instant periodEnd = toInstant(item.getCurrentPeriodEnd());

        String planId = resolvePlanId(item.getPrice());

        subscriptionService.updateSubscription(
                subscription.getId(),status,periodStart,periodEnd,subscription.getCancelAtPeriodEnd(),planId
        );
    }

    private void handleCustomerSubscriptionDeleted(com.stripe.model.Subscription subscription) {
        if(subscription==null){
            log.error("Subscription object is null inside handleCustomerSubscriptionDeleted");
            return;
        }

        subscriptionService.cancelSubscription(subscription.getId());


    }

    private void handleInvoicePaid(Invoice invoice) {
        String subId = extractSubscriptionId(invoice);
        if(subId == null) return;

        try {
            com.stripe.model.Subscription subscription = Subscription.retrieve(subId); // sdk calling the stripe server

            SubscriptionItem item = subscription.getItems().getData().get(0);
            Instant periodStart = toInstant(item.getCurrentPeriodStart());
            Instant periodEnd = toInstant(item.getCurrentPeriodEnd());

            subscriptionService.renewSubscriptionPeriod(
                    subId,
                    periodStart,
                    periodEnd
            );

        } catch (StripeException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleInvoicePaymentFailed(Invoice invoice) {
        String subId = extractSubscriptionId(invoice);
        if(subId == null) return;

        subscriptionService.markSubscriptionPastDue(subId);
    }
// ***************Utility Methods*******************************

    private User getUser(String userId) {
        return userRepository.findById(userId).orElseThrow(
                () -> new ResourceNotFoundException("User",userId.toString())
        );
    }

    private SubscriptionStatus mapStripeStatusToEnum(String status) {
        return switch(status){
            case "active" -> SubscriptionStatus.ACTIVE;
            case "trialing" -> SubscriptionStatus.TRAILING;
            case "past_due","unpaid","paused","incomplete_expired" -> SubscriptionStatus.PAST_DUE;
            case "canceled" -> SubscriptionStatus.CANCELLED;
            case "incomplete" -> SubscriptionStatus.INCOMPLETE;
            default -> {
                log.warn("Unmapped Stripe Status: {}",status);
                yield null;
            }
        };
    }

    private Instant toInstant(Long epoc) {
        return epoc != null ? Instant.ofEpochSecond(epoc) : null;
    }

    private String resolvePlanId(Price price) {
        if(price == null || price.getId() == null) return null;

        return planRepository.findByStripePriceId(price.getId())
                .map(Plan::getId)
                .orElse(null);
    }

    private String extractSubscriptionId(Invoice invoice){
        var parent = invoice.getParent();
        if(parent == null) return null;

        var subDetails = parent.getSubscriptionDetails();
        if(subDetails==null) return null;

        return subDetails.getSubscription();
    }
}
