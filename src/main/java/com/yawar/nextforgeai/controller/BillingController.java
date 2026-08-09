package com.yawar.nextforgeai.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.yawar.nextforgeai.dto.CheckoutSessionUrlRequest;
import com.yawar.nextforgeai.dto.CheckoutSessionUrlResponse;
import com.yawar.nextforgeai.dto.PortalResponse;
import com.yawar.nextforgeai.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/payments")
@RequiredArgsConstructor
public class BillingController {

    private final PaymentService paymentService;


    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @PostMapping("/checkout-seesion-url")
    public ResponseEntity<CheckoutSessionUrlResponse> checkoutSessionUrl(@RequestBody(required = true)CheckoutSessionUrlRequest checkoutSessionUrlRequest){
        CheckoutSessionUrlResponse checkoutSessionUrlResponse = paymentService.createCheckoutSessionUrl(checkoutSessionUrlRequest);
        return ResponseEntity.ok(checkoutSessionUrlResponse);
    }


    @PostMapping("/portal")
    public ResponseEntity<PortalResponse> openCustomerPortal(){
        return ResponseEntity.ok(paymentService.openCustomerPortal());
    }


    @PostMapping("/webhooks/payment")
    public ResponseEntity<String> handlePaymentWebhooks(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader
    ){
        try {
            Event event = Webhook.constructEvent(payload,sigHeader,webhookSecret);

            EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
            StripeObject stripeObject = null;

            if(deserializer.getObject().isPresent()){
                stripeObject = deserializer.getObject().get();
            }else{
                // Fallback: Deserialize from raw JSON
                try{
                    stripeObject = deserializer.deserializeUnsafe();
                    if(stripeObject == null){
                        log.warn("Failed to deserialize webhook object for event :{}",event.getType());
                        return ResponseEntity.ok().build();
                    }
                }catch (Exception e){
                    log.error("Unsafe deserialize failed for event {} : {}",event.getType(),e.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Deserialization failed");
                }
            }

//            NOW extract metadata only if it's a checkout session
            Map<String,String> metadata = new HashMap<>();
            if(stripeObject instanceof Session session){
                metadata = session.getMetadata();
            }

//            pass to your processor
            paymentService.handleWebhookEvent(event.getType(),stripeObject,metadata);

            return ResponseEntity.ok().build();
        } catch (SignatureVerificationException e) {
            throw new RuntimeException(e);
        }
    }
}
