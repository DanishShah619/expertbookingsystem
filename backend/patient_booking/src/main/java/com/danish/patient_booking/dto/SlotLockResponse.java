package com.danish.patient_booking.dto;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class SlotLockResponse {
    private String lockToken;
    private Instant expiresAt;
    private String clientSecret;   // Stripe Payment Element needs this
    private Long amountInCents;  // display amount (INR — no cents, just ₹)
    private String currency;
}