package com.danish.patient_booking.service;

import com.danish.patient_booking.dto.BookingDto;
import com.danish.patient_booking.enums.BookingStatus;
import com.danish.patient_booking.enums.PaymentStatus;
import com.danish.patient_booking.enums.Status;
import com.danish.patient_booking.exception.ResourceNotFoundException;
import com.danish.patient_booking.exception.SlotNotAvailableException;
import com.danish.patient_booking.model.Booking;
import com.danish.patient_booking.model.Payment;
import com.danish.patient_booking.model.SeatLock;
import com.danish.patient_booking.model.TimeSlot;
import com.danish.patient_booking.model.User;
import com.danish.patient_booking.repository.BookingRepository;
import com.danish.patient_booking.repository.PaymentRepository;
import com.danish.patient_booking.repository.SeatLockRepository;
import com.danish.patient_booking.repository.TimeSlotRepository;
import com.danish.patient_booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import com.danish.patient_booking.util.AppLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private static final AppLogger log = AppLogger.getLogger(BookingService.class);


    @Autowired
    @Lazy
    private BookingService self;
    private final BookingRepository bookingRepo;
    private final SeatLockRepository seatLockRepo;
    private final TimeSlotRepository slotRepo;
    private final PaymentRepository paymentRepo;
    private final StripeService stripeService;
    private final WebSocketNotificationService notificationService;
    private final UserRepository userRepo;

    @Value("${app.booking.cancel-cutoff-minutes:60}")
    private long cancelCutoffMinutes;

    
    public void confirmBooking(String paymentIntentId) {

        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║ CONFIRM BOOKING STARTED for: {} ║", paymentIntentId);
        log.info("╚══════════════════════════════════════════════════════════════╝");

        // ========== STEP 1: RETRY LOOP TO FIND RECORDS ==========

        log.info("confirmBooking started for paymentIntentId={}", paymentIntentId);

        SeatLock lock = null;
        Payment payment = null;

        int maxRetries = 3;       // was 10 — records are always present after fix
        int retryDelayMs = 200;   // was 500ms

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            lock = seatLockRepo.findByPaymentIntentId(paymentIntentId).orElse(null);
            payment = paymentRepo.findByStripePaymentIntentId(paymentIntentId).orElse(null);

            if (lock != null || payment != null) {
                log.info("Records found on attempt {}/{}", attempt, maxRetries);
                break;
            }

            if (attempt < maxRetries) {
                log.warn("Records not found, retrying in {}ms (attempt {}/{})",
                        retryDelayMs, attempt, maxRetries);
                try {
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Retry interrupted for paymentIntentId={}", paymentIntentId);
                    return;
                }
            } else {
                log.error("MANUAL REVIEW: no records found after {} attempts for paymentIntentId={}",
                        maxRetries, paymentIntentId);
                queueForManualReview(paymentIntentId);
                return;
            }
        }

        // Delegate to transactional method via Spring proxy (not `this.`)
        self.doConfirmBooking(paymentIntentId);
    }

    @Transactional
    public void doConfirmBooking(String paymentIntentId) {
        SeatLock lock    = seatLockRepo.findByPaymentIntentId(paymentIntentId).orElse(null);
        Payment  payment = paymentRepo.findByStripePaymentIntentId(paymentIntentId).orElse(null);


        // ========== STEP 2: IDEMPOTENCY CHECK ==========
        log.info("[STEP 2] Checking if booking already exists for paymentIntentId: {}", paymentIntentId);
        if (bookingRepo.existsByPaymentIntentId(paymentIntentId)) {
            log.warn("[STEP 2] ⚠️ Booking already exists for paymentIntentId={} - skipping duplicate webhook", paymentIntentId);
            return;
        }
        log.info("[STEP 2] ✅ No existing booking found - proceeding");

        // ========== STEP 3: RESOLVE CONTEXT ==========
        log.info("[STEP 3] Resolving booking context from lock/payment");
        TimeSlot paymentSlot = lock != null ? lock.getSlot() : payment != null ? payment.getSlot() : null;
        User user = lock != null ? lock.getUser() : payment != null ? payment.getUser() : null;

        log.info("[STEP 3] paymentSlot: {}, user: {}", paymentSlot != null ? paymentSlot.getId() : "null",
                user != null ? user.getId() : "null");

        if (paymentSlot == null || user == null) {
            log.error("[STEP 3] ❌ Cannot resolve context - paymentSlot={}, user={}", paymentSlot, user);
            throw new IllegalStateException("Cannot resolve paid booking context for paymentIntentId=" + paymentIntentId);
        }
        log.info("[STEP 3] ✅ Context resolved - slotId={}, userId={}", paymentSlot.getId(), user.getId());

        // ========== STEP 4: FETCH SLOT WITH LOCK ==========
        log.info("[STEP 4] Fetching slot with pessimistic lock for slotId: {}", paymentSlot.getId());
        TimeSlot slot = slotRepo.findByIdWithLock(paymentSlot.getId())
                .orElseThrow(() -> {
                    log.error("[STEP 4] ❌ Slot not found: {}", paymentSlot.getId());
                    return new ResourceNotFoundException("Slot not found: " + paymentSlot.getId());
                });
        log.info("[STEP 4] ✅ Slot found - id={}, status={}, expertId={}, startTime={}",
                slot.getId(), slot.getStatus(), slot.getExpert().getId(), slot.getStartTime());

        // ========== STEP 5: CHECK TIMING ==========
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = lock != null ? lock.getExpiresAt() : payment != null ? payment.getExpiresAt() : null;

        log.info("[STEP 5] Current time: {}", now);
        log.info("[STEP 5] Lock expires at: {}", expiresAt);
        if (expiresAt != null) {
            log.info("[STEP 5] Lock expired: {}", expiresAt.isBefore(now));
            log.info("[STEP 5] Minutes until expiry: {}", java.time.Duration.between(now, expiresAt).toMinutes());
        }

        // ========== STEP 6: CHECK PAYMENT TERMINAL STATE ==========
        if (payment != null && isTerminalWithoutBooking(payment.getStatus())) {
            log.warn("[STEP 6] ⚠️ Payment in terminal state: {}", payment.getStatus());
            refundAndMark(payment, paymentIntentId, slot, user, expiresAt,
                    "Payment succeeded after it was already " + payment.getStatus());
            releaseLockIfPresent(lock, slot);
            log.info("[STEP 6] ✅ Handled terminal payment state - returning");
            return;
        }
        log.info("[STEP 6] ✅ Payment status is OK: {}", payment != null ? payment.getStatus() : "null");

        // ========== STEP 7: CHECK SLOT ALREADY BOOKED ==========
        log.info("[STEP 7] Checking if slot {} is already BOOKED", slot.getId());
        boolean slotBooked = slot.getStatus() == Status.BOOKED;
        boolean bookingExists = bookingRepo.existsBySlotIdAndStatus(slot.getId(), BookingStatus.CONFIRMED);

        log.info("[STEP 7] Slot status BOOKED: {}, Existing confirmed booking: {}", slotBooked, bookingExists);

        if (slotBooked || bookingExists) {
            log.warn("[STEP 7] ⚠️ Slot already has a confirmed booking - refunding");
            refundAndMark(payment, paymentIntentId, slot, user, expiresAt, "Slot already has another booking");
            releaseLockIfPresent(lock, slot);
            log.info("[STEP 7] ✅ Handled already-booked slot - returning");
            return;
        }
        log.info("[STEP 7] ✅ Slot is available for booking");

        // ========== STEP 8: CHECK LOCK EXPIRY ==========
        if (expiresAt != null && expiresAt.isBefore(now)) {
            log.warn("[STEP 8] ⚠️ Lock has expired! expiresAt={}, now={}", expiresAt, now);

            boolean lockedBySomeoneElse = seatLockRepo.findBySlot_Id(slot.getId())
                    .map(activeLock -> {
                        boolean differentUser = !activeLock.getUser().getId().equals(user.getId());
                        log.info("[STEP 8] Active lock found - userId={}, differentUser={}",
                                activeLock.getUser().getId(), differentUser);
                        return differentUser;
                    })
                    .orElse(false);

            if (lockedBySomeoneElse) {
                log.warn("[STEP 8] ❌ Lock expired AND slot locked by another user - refunding");
                refundAndMark(payment, paymentIntentId, slot, user, expiresAt, "Lock expired and slot was locked by another user");
                releaseLockIfPresent(lock, slot);
                return;
            }
            log.info("[STEP 8] ✅ Lock expired but slot still available/same user - proceeding with booking");
        } else {
            log.info("[STEP 8] ✅ Lock is still valid (or no lock expiry)");
        }

        // ========== STEP 9: CREATE BOOKING ==========
        log.info("[STEP 9] Creating booking record...");
        slot.setStatus(Status.BOOKED);
        TimeSlot savedSlot = slotRepo.save(slot);
        log.info("[STEP 9] ✅ Slot status updated to BOOKED - slotId={}", savedSlot.getId());

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setSlot(slot);
        booking.setPaymentIntentId(paymentIntentId);
        booking.setAmountPaid(payment != null ? payment.getAmount() : slot.getExpert().getSessionPrice());
        booking.setCurrency(payment != null ? payment.getCurrency() : slot.getExpert().getCurrency());
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setBookedAt(LocalDateTime.now());

        log.info("[STEP 9] Booking entity prepared - userId={}, slotId={}, amount={}, currency={}",
                user.getId(), slot.getId(), booking.getAmountPaid(), booking.getCurrency());

        Booking savedBooking = bookingRepo.save(booking);
        log.info("[STEP 9] ✅ Booking saved with id={}", savedBooking.getId());

        // ========== STEP 10: UPDATE PAYMENT AUDIT ==========
        log.info("[STEP 10] Updating payment audit record");
        upsertPaymentAudit(booking, paymentIntentId, slot, user, expiresAt, PaymentStatus.SUCCEEDED);
        log.info("[STEP 10] ✅ Payment audit updated");

        // ========== STEP 11: CLEANUP ==========
        if (lock != null) {
            log.info("[STEP 11] Deleting SeatLock record - id={}", lock.getId());
            seatLockRepo.delete(lock);
            log.info("[STEP 11] ✅ SeatLock deleted");
        } else {
            log.info("[STEP 11] No SeatLock to delete");
        }

        // ========== STEP 12: BROADCAST ==========
        log.info("[STEP 12] Broadcasting slot update via WebSocket");
        notificationService.broadcastSlotUpdate(slot, null);
        log.info("[STEP 12] ✅ WebSocket broadcast sent");


    }

    // Helper method for manual review queue
    
    private void queueForManualReview(String paymentIntentId) {
        log.error("MANUAL REVIEW NEEDED: paymentIntentId={} - webhook received but no records found after retries",
                paymentIntentId);
    }


    @Transactional
    public void handlePaymentFailure(String paymentIntentId) {
        seatLockRepo.findByPaymentIntentId(paymentIntentId).ifPresentOrElse(
                lock -> {
                    TimeSlot slot = lock.getSlot();
                    slot.setStatus(Status.AVAILABLE);
                    slotRepo.save(slot);
                    upsertPaymentAudit(null, paymentIntentId, slot, lock.getUser(), lock.getExpiresAt(), PaymentStatus.FAILED);
                    seatLockRepo.delete(lock);
                    notificationService.broadcastSlotUpdate(slot, null);
                    log.info("Payment failed - slotId={} released to AVAILABLE", slot.getId());
                },
                () -> paymentRepo.findByStripePaymentIntentId(paymentIntentId)
                        .ifPresentOrElse(
                                payment -> {
                                    payment.setStatus(PaymentStatus.FAILED);
                                    paymentRepo.save(payment);
                                },
                                () -> log.warn("Payment failure webhook: no lock or payment found for paymentIntentId={}", paymentIntentId)
                        )
        );
    }

    @Transactional
    public void cancelBooking(Long bookingId, String googleId) {
        User user = userRepo.findByGoogleId(googleId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (!booking.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Booking not found: " + bookingId);
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new SlotNotAvailableException("Booking is already cancelled");
        }

        TimeSlot slot = booking.getSlot();
        LocalDateTime cancellationDeadline = slot.getStartTime().minusMinutes(cancelCutoffMinutes);
        if (!LocalDateTime.now().isBefore(cancellationDeadline)) {
            throw new SlotNotAvailableException(
                    "Booking can only be cancelled at least " + cancelCutoffMinutes + " minutes before the session"
            );
        }

        try {
            stripeService.refundPaymentIntent(booking.getPaymentIntentId());
        } catch (Exception e) {
            log.error("Refund failed for bookingId={}: {}", bookingId, e.getMessage());
            throw new RuntimeException("Refund failed - please contact support");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepo.save(booking);

        paymentRepo.findByStripePaymentIntentId(booking.getPaymentIntentId())
                .ifPresent(payment -> {
                    payment.setStatus(PaymentStatus.REFUNDED);
                    payment.setUpdatedAt(LocalDateTime.now());
                    paymentRepo.save(payment);
                });

        if (slot.getStatus() == Status.BOOKED) {
            slot.setStatus(Status.AVAILABLE);
            slotRepo.save(slot);
            notificationService.broadcastSlotUpdate(slot, null);
        }

        log.info("Booking CANCELLED - bookingId={} slotId={} refunded", bookingId, slot.getId());
    }

    @Transactional(readOnly = true)
    public List<BookingDto> getMyBookings(String googleId) {
        User user = userRepo.findByGoogleId(googleId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return bookingRepo.findByUserIdOrderByBookedAtDesc(user.getId())
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BookingDto> getAllBookings() {
        return bookingRepo.findAllByOrderByBookedAtDesc()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private BookingDto toDto(Booking booking) {
        return BookingDto.builder()
                .id(booking.getId())
                .slotId(booking.getSlot().getId())
                .expertName(booking.getSlot().getExpert().getName())
                .expertId(booking.getSlot().getExpert().getId())
                .startTime(booking.getSlot().getStartTime())
                .endTime(booking.getSlot().getEndTime())
                .amountPaid(booking.getAmountPaid())
                .currency(booking.getCurrency())
                .status(booking.getStatus().name())
                .bookedAt(booking.getBookedAt())
                .paymentIntentId(booking.getPaymentIntentId())
                .build();
    }

    private void upsertPaymentAudit(Booking booking,
                                    String paymentIntentId,
                                    TimeSlot slot,
                                    User user,
                                    LocalDateTime expiresAt,
                                    PaymentStatus status) {
        Payment payment = paymentRepo.findByStripePaymentIntentId(paymentIntentId)
                .orElseGet(Payment::new);

        payment.setBooking(booking);
        payment.setUser(user);
        payment.setSlot(slot);
        payment.setStripePaymentIntentId(paymentIntentId);
        if (payment.getAmount() == null) {
            payment.setAmount(slot.getExpert().getSessionPrice());
            payment.setCurrency(slot.getExpert().getCurrency());
        }
        payment.setStatus(status);
        payment.setExpiresAt(expiresAt);
        payment.setUpdatedAt(LocalDateTime.now());

        if (payment.getCreatedAt() == null) {
            payment.setCreatedAt(LocalDateTime.now());
        }

        paymentRepo.save(payment);
    }

    private boolean isTerminalWithoutBooking(PaymentStatus status) {
        return status == PaymentStatus.CANCELLED
                || status == PaymentStatus.FAILED
                || status == PaymentStatus.REFUNDED
                || status == PaymentStatus.EXPIRED;
    }

    private void refundAndMark(Payment payment,
                               String paymentIntentId,
                               TimeSlot slot,
                               User user,
                               LocalDateTime expiresAt,
                               String reason) {
        log.warn("{} for paymentIntentId={} - refunding", reason, paymentIntentId);
        try {
            stripeService.refundPaymentIntent(paymentIntentId);
            if (payment != null) {
                payment.setStatus(PaymentStatus.REFUNDED);
                paymentRepo.save(payment);
            } else {
                upsertPaymentAudit(null, paymentIntentId, slot, user, expiresAt, PaymentStatus.REFUNDED);
            }
        } catch (Exception e) {
            if (payment != null) {
                payment.setStatus(PaymentStatus.REFUND_FAILED);
                paymentRepo.save(payment);
            }
            throw new RuntimeException("Refund failed for paymentIntentId=" + paymentIntentId, e);
        }
    }

    private void releaseLockIfPresent(SeatLock lock, TimeSlot slot) {
        if (lock != null) {
            seatLockRepo.delete(lock);
        }
        if (slot.getStatus() == Status.LOCKED) {
            slot.setStatus(Status.AVAILABLE);
            slotRepo.save(slot);
            notificationService.broadcastSlotUpdate(slot, null);
        }
    }
}
