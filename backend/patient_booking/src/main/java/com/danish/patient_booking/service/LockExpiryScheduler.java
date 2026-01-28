package com.danish.patient_booking.service;

import com.danish.patient_booking.model.SeatLock;
import com.danish.patient_booking.model.TimeSlot;
import com.danish.patient_booking.enums.Status;
import com.danish.patient_booking.repository.SeatLockRepository;
import com.danish.patient_booking.repository.TimeSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class LockExpiryScheduler {

    private final SeatLockRepository          seatLockRepo;
    private final TimeSlotRepository          slotRepo;
    private final StripeService               stripeService;
    private final WebSocketNotificationService notificationService;

    /**
     * Runs every 60 seconds.
     * Scans for SeatLock rows whose expiresAt has passed
     * and releases them back to AVAILABLE.
     *
     * Handles the case where a user locks a slot but
     * abandons the payment page without paying or cancelling.
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expireStaleLocks() {

        List<SeatLock> expiredLocks =
                seatLockRepo.findAllByExpiresAtBefore(LocalDateTime.now());

        if (expiredLocks.isEmpty()) {
            log.debug("Lock expiry sweep: no expired locks found");
            return;
        }

        log.info("Lock expiry sweep: found {} expired lock(s)", expiredLocks.size());

        for (SeatLock lock : expiredLocks) {
            try {
                processExpiredLock(lock);
            } catch (Exception e) {
                // CRITICAL: catch per-lock so one failure doesn't
                // stop the rest of the locks from being released
                log.error("Failed to expire lock id={} slotId={}: {}",
                        lock.getId(), lock.getSlot().getId(), e.getMessage());
            }
        }
    }

    private void processExpiredLock(SeatLock lock) {
        TimeSlot slot = lock.getSlot();

        // Guard: only release if still LOCKED
        // Could be BOOKED if webhook arrived just before scheduler ran
        if (slot.getStatus() != Status.LOCKED) {
            log.info("Skipping expired lock id={} — slot {} is already {}",
                    lock.getId(), slot.getId(), slot.getStatus());
            seatLockRepo.delete(lock);
            return;
        }

        // 1. Release slot back to AVAILABLE
        slot.setStatus(Status.AVAILABLE);
        slotRepo.save(slot);

        // 2. Cancel the Stripe PaymentIntent
        //    so user cannot complete payment after lock expired
        if (lock.getPaymentIntentId() != null) {
            stripeService.cancelPaymentIntent(lock.getPaymentIntentId());
        }

        // 3. Delete the lock row
        seatLockRepo.delete(lock);

        // 4. Broadcast to all WebSocket clients — slot turns green again
        notificationService.broadcastSlotUpdate(slot, null);

        log.info("Expired lock released — slotId={} is now AVAILABLE", slot.getId());
    }
}