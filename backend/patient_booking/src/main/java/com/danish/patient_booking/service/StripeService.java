package com.danish.patient_booking.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCancelParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import jakarta.annotation.PostConstruct;
import com.danish.patient_booking.util.AppLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

@Service
public class StripeService {

    private static final AppLogger log = AppLogger.getLogger(StripeService.class);

    @Value("${stripe.secret-key}")
    private String secretKey;

    /**
     * Initialises Stripe SDK with your secret key at startup.
     * All Stripe API calls after this point use this key automatically.
     */
    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
        log.info("Stripe SDK initialised");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE PAYMENT INTENT
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates a Stripe PaymentIntent for the session price.
     *
     * toStripeAmount() converts major units from the database into the
     * minor-unit amount expected by Stripe.
     *
     * The clientSecret returned here is sent to the frontend
     * so Stripe.js can render the Payment Element.
     */
    public PaymentIntent createPaymentIntent(BigDecimal amount,
                                             String currency,
                                             Long userId,
                                             Long slotId) throws StripeException {

        long stripeAmount = toStripeAmount(amount, currency);

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(stripeAmount)
                .setCurrency(currency.toLowerCase())
                // Metadata lets you trace this PaymentIntent back to your DB records
                .putMetadata("userId", String.valueOf(userId))
                .putMetadata("slotId", String.valueOf(slotId))
                // Automatically shows the best payment methods for the currency/country
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .build();

        PaymentIntent intent = PaymentIntent.create(params);

        log.info("Created PaymentIntent id={} amount={} {}",
                intent.getId(), stripeAmount, currency.toUpperCase());

        return intent;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CANCEL PAYMENT INTENT
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Called when:
     * - Lock expires (scheduler)
     * - User manually releases lock
     *
     * Safe to call even if PaymentIntent is already cancelled —
     * we catch and log rather than throw, so the lock release
     * still completes even if Stripe is temporarily unreachable.
     */
    public boolean cancelPaymentIntent(String paymentIntentId) {
        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);

            // Can only cancel if in a cancellable state
            // succeeded/canceled intents will throw — we handle below
            if (isCancellable(intent.getStatus())) {
                intent.cancel(PaymentIntentCancelParams.builder()
                        .setCancellationReason(
                                PaymentIntentCancelParams.CancellationReason.ABANDONED
                        )
                        .build()
                );
                log.info("Cancelled PaymentIntent id={}", paymentIntentId);
            } else {
                log.info("PaymentIntent id={} already in status={} — skipping cancel",
                        paymentIntentId, intent.getStatus());
            }

            return true;

        } catch (StripeException e) {
            // Non-fatal — log and continue. Lock release must not
            // be blocked by a Stripe API hiccup.
            log.warn("Could not cancel PaymentIntent id={}: {}",
                    paymentIntentId, e.getMessage());
            return false;
        }
    }

    public String getPaymentIntentStatus(String paymentIntentId) throws StripeException {
        return PaymentIntent.retrieve(paymentIntentId).getStatus();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REFUND PAYMENT INTENT
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Called when:
     * - Lock expired but webhook arrived anyway (edge case)
     * - User cancels a CONFIRMED booking
     *
     * Full refund — refunds the entire amount charged.
     */
    public void refundPaymentIntent(String paymentIntentId) throws StripeException {
        RefundCreateParams params = RefundCreateParams.builder()
                .setPaymentIntent(paymentIntentId)
                .build();

        Refund refund = Refund.create(params);

        log.info("Refunded PaymentIntent id={} refundId={} status={}",
                paymentIntentId, refund.getId(), refund.getStatus());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Converts your DB amount to what Stripe expects.
     *
     * Most currencies (USD, EUR, GBP, INR): multiply by 100
     *   $10.00 -> 1000 cents
     *
     * Zero-decimal currencies (JPY, KRW): send as-is
     *   JPY 500 -> 500
     *
     * Full list: https://stripe.com/docs/currencies#zero-decimal
     */
    private long toStripeAmount(BigDecimal amount, String currency) {
        String cur = currency.toUpperCase();
        Set<String> zeroDecimalCurrencies = Set.of(
                "BIF", "CLP", "DJF", "GNF", "JPY",
                "KMF", "KRW", "MGA", "PYG", "RWF", "UGX",
                "VND", "VUV", "XAF", "XOF", "XPF"
        );
        Set<String> threeDecimalCurrencies = Set.of(
                "BHD", "IQD", "JOD", "KWD", "LYD", "OMR", "TND"
        );

        if (zeroDecimalCurrencies.contains(cur)) {
            return amount.setScale(0, RoundingMode.HALF_UP).longValueExact();
        }

        if (threeDecimalCurrencies.contains(cur)) {
            return amount.multiply(BigDecimal.valueOf(1000))
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValueExact();
        }

        return amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    /**
     * Stripe only allows cancellation from these statuses.
     * Attempting to cancel a 'succeeded' or already 'canceled'
     * intent throws an InvalidRequestException.
     */
    private boolean isCancellable(String status) {
        return Set.of(
                "requires_payment_method",
                "requires_capture",
                "requires_confirmation",
                "requires_action",
                "processing"
        ).contains(status);
    }
}
