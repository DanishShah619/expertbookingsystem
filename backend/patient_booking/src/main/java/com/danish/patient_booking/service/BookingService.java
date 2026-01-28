package com.danish.patient_booking.service;

import com.danish.patient_booking.dto.BookingDto;
import com.danish.patient_booking.enums.*;
import com.danish.patient_booking.exception.*;
import com.danish.patient_booking.model.Booking;
import com.danish.patient_booking.model.Payment;
import com.danish.patient_booking.model.SeatLock;
import com.danish.patient_booking.model.TimeSlot;
import com.danish.patient_booking.model.User;
import com.danish.patient_booking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository            bookingRepo;
    private final SeatLockRepository           seatLockRepo;
    private final TimeSlotRepository           slotRepo;
    private final PaymentRepository            paymentRepo;
    private final StripeService                stripeService;
    private final WebSocketNotificationService notificationService;
    private final UserRepository               userRepo;

    // ─────────────────────────────────────────────────────────────────────────
    // CONFIRM BOOKING — called from StripeWebhookController
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Triggered by Stripe webhook: payment_intent.succeeded
     *
     * Key design decisions:
     * 1. Idempotency check first — Stripe may deliver the same webhook twice.
     *    If booking already exists we return silently (don't create duplicate).
     * 2. If lock expired between payment and webhook delivery → refund immediately.
     * 3. Everything in one @Transactional — if anything fails, DB rolls back.
     */
    @Transactional
    public void confirmBooking(String paymentIntentId) {

        // 1. IDEMPOTENCY GUARD
        //    Stripe retries webhooks for 3 days — must handle duplicates
        if (bookingRepo.existsByPaymentIntentId(paymentIntentId)) {
            log.info("Booking already exists for paymentIntentId={} — skipping duplicate webhook",
                    paymentIntentId);
            return;
        }

        // 2. Find the SeatLock for this payment
        //    Use Optional — scheduler may have already deleted it (race condition)
        SeatLock lock = seatLockRepo.findByPaymentIntentId(paymentIntentId)
                .orElse(null);

        if (lock == null) {
            log.warn("No active SeatLock found for paymentIntentId={} - already expired or processed",
                    paymentIntentId);
            return;
        }

        // 3. RACE CONDITION GUARD
        //    Lock expired AND webhook arrived in the same window
        //    Refund the user immediately — they paid for a slot they can't have
        if (lock.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Lock expired before webhook arrived for paymentIntentId={} — refunding",
                    paymentIntentId);
            try {
                stripeService.refundPaymentIntent(paymentIntentId);
            } catch (Exception e) {
                log.error("Refund failed for expired lock paymentIntentId={}: {}",
                        paymentIntentId, e.getMessage());
            }
            seatLockRepo.delete(lock);
            return;
        }

        TimeSlot slot = lock.getSlot();
        User     user = lock.getUser();

        // 4. Transition slot → BOOKED
        slot.setStatus(Status.BOOKED);
        slotRepo.save(slot);

        // 5. Create Booking record
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setSlot(slot);
        booking.setPaymentIntentId(paymentIntentId);
        booking.setAmountPaid(slot.getExpert().getSessionPrice());
        booking.setCurrency(slot.getExpert().getCurrency());
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setBookedAt(LocalDateTime.now());
        bookingRepo.save(booking);

        // 6. Create Payment audit record
        //    Separate from Booking so you have a full Stripe audit trail
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setStripePaymentIntentId(paymentIntentId);
        payment.setAmount(booking.getAmountPaid());
        payment.setCurrency(booking.getCurrency());
        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepo.save(payment);

        // 7. Clean up SeatLock — no longer needed
        seatLockRepo.delete(lock);

        // 8. Broadcast BOOKED to all WebSocket subscribers
        //    Slot turns grey on everyone's screen immediately
        notificationService.broadcastSlotUpdate(slot, null);

        log.info("Booking CONFIRMED — bookingId={} slotId={} userId={} amount={} {}",
                booking.getId(), slot.getId(), user.getId(),
                booking.getAmountPaid(), booking.getCurrency());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HANDLE PAYMENT FAILURE — called from StripeWebhookController
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Triggered by Stripe webhook: payment_intent.payment_failed
     * Releases the slot back to AVAILABLE so others can book it.
     */
    @Transactional
    public void handlePaymentFailure(String paymentIntentId) {

        seatLockRepo.findByPaymentIntentId(paymentIntentId).ifPresentOrElse(
                lock -> {
                    TimeSlot slot = lock.getSlot();
                    slot.setStatus(Status.AVAILABLE);
                    slotRepo.save(slot);
                    seatLockRepo.delete(lock);
                    notificationService.broadcastSlotUpdate(slot, null);
                    log.info("Payment failed — slotId={} released to AVAILABLE", slot.getId());
                },
                () -> log.warn("Payment failure webhook: no lock found for paymentIntentId={}",
                        paymentIntentId)
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CANCEL BOOKING — called from BookingController (user cancels)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * User cancels a CONFIRMED booking.
     * Refunds via Stripe and releases the slot back to AVAILABLE.
     */
    @Transactional
    public void cancelBooking(Long bookingId, String googleId) {

        User user = userRepo.findByGoogleId(googleId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        // Guard: users can only cancel their own bookings
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Booking not found: " + bookingId);
            // Intentionally vague — don't reveal other users' booking IDs
        }

        // Guard: can only cancel CONFIRMED bookings
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new SlotNotAvailableException("Booking is already cancelled");
        }

        // 1. Issue Stripe refund
        try {
            stripeService.refundPaymentIntent(booking.getPaymentIntentId());
        } catch (Exception e) {
            log.error("Refund failed for bookingId={}: {}", bookingId, e.getMessage());
            throw new RuntimeException("Refund failed — please contact support");
        }

        // 2. Update booking status
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepo.save(booking);

        // 3. Update payment record
        paymentRepo.findByStripePaymentIntentId(booking.getPaymentIntentId())
                .ifPresent(payment -> {
                    payment.setStatus(PaymentStatus.REFUNDED);
                    payment.setUpdatedAt(LocalDateTime.now());
                    paymentRepo.save(payment);
                });

        // 4. Release slot back to AVAILABLE
        TimeSlot slot = booking.getSlot();
        slot.setStatus(Status.AVAILABLE);
        slotRepo.save(slot);

        // 5. Broadcast slot available again
        notificationService.broadcastSlotUpdate(slot, null);

        log.info("Booking CANCELLED — bookingId={} slotId={} refunded", bookingId, slot.getId());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET MY BOOKINGS — called from BookingController
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<BookingDto> getMyBookings(String googleId) {
        User user = userRepo.findByGoogleId(googleId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return bookingRepo.findByUserIdOrderByBookedAtDesc(user.getId())
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET ALL BOOKINGS — admin only
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<BookingDto> getAllBookings() {
        return bookingRepo.findAllByOrderByBookedAtDesc()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MAPPER
    // ─────────────────────────────────────────────────────────────────────────

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
                .build();
    }
}
