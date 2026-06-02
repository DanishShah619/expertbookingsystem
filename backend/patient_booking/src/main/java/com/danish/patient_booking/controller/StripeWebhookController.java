package com.danish.patient_booking.controller;

import com.danish.patient_booking.config.RawBodyCachingFilter.CachedBodyHttpServletRequest;
import com.danish.patient_booking.service.BookingService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import com.danish.patient_booking.util.AppLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class StripeWebhookController {

    private static final AppLogger log = AppLogger.getLogger(StripeWebhookController.class);

    private final BookingService bookingService;

    @Value("${STRIPE_WEBHOOK_SECRET}")
    private String webhookSecret;

    @PostMapping("/stripe")
    public ResponseEntity<String> handleWebhook(
            HttpServletRequest request,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        // 1. Get raw bytes from our caching filter — NOT from @RequestBody
        byte[] rawBody;
        if (request instanceof CachedBodyHttpServletRequest cached) {
            rawBody = cached.getRawBody();
        } else {
            log.error("Raw body filter did not run — cannot verify Stripe signature");
            return ResponseEntity.status(500).body("Internal filter error");
        }

        // 2. Verify Stripe signature — rejects forged/tampered webhooks
        Event event;
        try {
            event = Webhook.constructEvent(
                    new String(rawBody, StandardCharsets.UTF_8), sigHeader, webhookSecret
            );
        } catch (SignatureVerificationException e) {
            log.warn("Invalid Stripe signature: {}", e.getMessage());
            return ResponseEntity.status(400).body("Invalid signature");
        }

        // 3. Handle events — always return 200 to Stripe even on business logic errors
        //    If you return non-200, Stripe retries for 3 days — causing duplicate bookings
        log.info("Stripe webhook received: {}", event.getType());

        try {
            switch (event.getType()) {

                case "payment_intent.succeeded" -> {
                    PaymentIntent intent = (PaymentIntent)
                            event.getDataObjectDeserializer()
                                    .getObject()
                                    .orElseThrow(() -> new RuntimeException("Cannot deserialize PaymentIntent"));
                    bookingService.confirmBooking(intent.getId());
                    log.info("Booking confirmed for paymentIntentId={}", intent.getId());
                }

                case "payment_intent.payment_failed" -> {
                    PaymentIntent intent = (PaymentIntent)
                            event.getDataObjectDeserializer()
                                    .getObject()
                                    .orElseThrow(() -> new RuntimeException("Cannot deserialize PaymentIntent"));
                    bookingService.handlePaymentFailure(intent.getId());
                    log.info("Payment failed for paymentIntentId={}", intent.getId());
                }

                default -> log.debug("Unhandled Stripe event type: {}", event.getType());
            }
        } catch (Exception e) {
            // CRITICAL: Log the error but still return 200 to Stripe
            // Otherwise Stripe retries → duplicate bookings
            log.error("Error processing Stripe event {}: {}", event.getType(), e.getMessage(), e);
            return ResponseEntity.ok("Webhook processing failed (logged)");
        }

        return ResponseEntity.ok("received");
    }
}
