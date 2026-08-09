package com.yawar.nextforgeai.service;

import com.stripe.model.StripeObject;
import com.yawar.nextforgeai.dto.CheckoutSessionUrlRequest;
import com.yawar.nextforgeai.dto.CheckoutSessionUrlResponse;
import com.yawar.nextforgeai.dto.PortalResponse;

import java.util.Map;

public interface PaymentService {
    CheckoutSessionUrlResponse createCheckoutSessionUrl(CheckoutSessionUrlRequest checkoutSessionUrlRequest);

    PortalResponse openCustomerPortal();

    void handleWebhookEvent(String type, StripeObject stripeObject, Map<String, String> metadata);

}
