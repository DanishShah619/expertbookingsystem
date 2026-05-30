package com.danish.patient_booking.service;

import com.danish.patient_booking.dto.SlotLockResponse;
import com.danish.patient_booking.enums.PaymentStatus;
import com.danish.patient_booking.enums.Status;
import com.danish.patient_booking.exception.*;
import com.danish.patient_booking.model.Payment;
import com.danish.patient_booking.model.SeatLock;
import com.danish.patient_booking.model.TimeSlot;
import com.danish.patient_booking.model.User;
import com.danish.patient_booking.repository.*;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import com.danish.patient_booking.util.AppLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SeatLockService {

    private static final AppLogger log = AppLogger.getLogger(SeatLockService.class);

    private final TimeSlotRepository    slotRepo;
    private final SeatLockRepository    seatLockRepo;
    private final UserRepository        userRepo;
    private final StripeService         stripeService;
    private final WebSocketNotificationService notificationService;
    private final PaymentRepository paymentRepo;

    @Value("${app.seat-lock.ttl-minutes:5}")
    private int lockTtlMinutes;

    // ─────────────────────────────────────────────────────────────────────────
    // LOCK A SLOT
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Atomically locks a slot for a user.
     *
     * Uses PESSIMISTIC_WRITE lock on the TimeSlot row so concurrent requests
     * from two users hitting the same slot are serialised at DB level.
     * Only one will proceed — the other gets SlotNotAvailableException.
     */
    @Transactional
    public SlotLockResponse lockSlot(Long slotId, String googleId) {

        // 1. Fetch slot with DB-level write lock — blocks other transactions
        TimeSlot slot = slotRepo.findByIdWithLock(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found: " + slotId));

        // 2. Guard: only AVAILABLE slots can be locked
        if (slot.getStatus() != Status.AVAILABLE) {
            throw new SlotNotAvailableException(
                    "Slot " + slotId + " is currently " + slot.getStatus()
            );
        }

        // 3. Guard: prevent same user locking the same slot twice
        //    (e.g. two browser tabs)
        User user = userRepo.findByGoogleId(googleId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        seatLockRepo.findBySlot_IdAndUser_Id(slotId, user.getId())
                .ifPresent(existingLock -> {
                    throw new SlotNotAvailableException("You already have a lock on this slot");
                });

        // 4. Transition slot → LOCKED
        slot.setStatus(Status.LOCKED);
        slotRepo.save(slot);

        // 5. Create Stripe PaymentIntent
        //    Amount is in INR — StripeService handles zero-decimal correctly
        PaymentIntent intent;
        try {
            intent = stripeService.createPaymentIntent(
                    slot.getExpert().getSessionPrice(),
                    slot.getExpert().getCurrency(),
                    user.getId(),
                    slotId
            );
        } catch (Exception e) {
            // CRITICAL: If Stripe fails, roll back the slot status
            // @Transactional handles this automatically on exception —
            // but we log it clearly for debugging
            log.error("Stripe PaymentIntent creation failed for slotId={}: {}", slotId, e.getMessage());
            throw new RuntimeException("Payment setup failed — please try again", e);
        }

        // 6. Persist SeatLock row
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(lockTtlMinutes);

        SeatLock lock = new SeatLock();
        lock.setSlot(slot);
        lock.setUser(user);
        lock.setLockToken(UUID.randomUUID().toString());
        lock.setPaymentIntentId(intent.getId());
        lock.setExpiresAt(expiresAt);
        seatLockRepo.save(lock);

        Payment payment = new Payment();
        payment.setUser(user);
        payment.setSlot(slot);
        payment.setStripePaymentIntentId(intent.getId());
        payment.setAmount(slot.getExpert().getSessionPrice());
        payment.setCurrency(slot.getExpert().getCurrency());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setExpiresAt(expiresAt);
        paymentRepo.save(payment);

        // 7. Broadcast LOCKED to all WebSocket subscribers
        //    Other users on the slot grid see it turn amber immediately
        notificationService.broadcastSlotUpdate(slot, lock.getExpiresAt());

        log.info("Slot {} locked by userId={} until {}", slotId, user.getId(), lock.getExpiresAt());

        return SlotLockResponse.builder()
                .lockToken(lock.getLockToken())
                .expiresAt(lock.getExpiresAt())
                .clientSecret(intent.getClientSecret())   // sent to Stripe.js in browser
                .amountInCents(intent.getAmount())        // for display — actual INR amount
                .currency(intent.getCurrency())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RELEASE A LOCK MANUALLY (user clicks cancel)
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void releaseLock(Long slotId, String googleId) {

        User user = userRepo.findByGoogleId(googleId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        SeatLock lock = seatLockRepo.findBySlot_IdAndUser_Id(slotId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No active lock found for this slot"));

        TimeSlot slot = lock.getSlot();

        // Only release if still LOCKED — may have already expired via scheduler
        if (slot.getStatus() == Status.LOCKED) {
            slot.setStatus(Status.AVAILABLE);
            slotRepo.save(slot);
        }

        // Cancel Stripe PaymentIntent so user is never charged
        stripeService.cancelPaymentIntent(lock.getPaymentIntentId());
        paymentRepo.findByStripePaymentIntentId(lock.getPaymentIntentId())
                .ifPresent(payment -> {
                    payment.setStatus(PaymentStatus.CANCELLED);
                    paymentRepo.save(payment);
                });

        seatLockRepo.delete(lock);

        // Broadcast slot is available again
        notificationService.broadcastSlotUpdate(slot, null);

        log.info("Lock manually released for slotId={} by userId={}", slotId, user.getId());
    }
}
