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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private static final AppLogger log = AppLogger.getLogger(BookingService.class);

    private final BookingRepository bookingRepo;
    private final SeatLockRepository seatLockRepo;
    private final TimeSlotRepository slotRepo;
    private final PaymentRepository paymentRepo;
    private final StripeService stripeService;
    private final WebSocketNotificationService notificationService;
    private final UserRepository userRepo;

    @Value("${app.booking.cancel-cutoff-minutes:60}")
    private long cancelCutoffMinutes;

    @Transactional
    public void confirmBooking(String paymentIntentId) {
        if (bookingRepo.existsByPaymentIntentId(paymentIntentId)) {
            log.info("Booking already exists for paymentIntentId={} - skipping duplicate webhook", paymentIntentId);
            return;
        }

        SeatLock lock = seatLockRepo.findByPaymentIntentId(paymentIntentId).orElse(null);
        Payment payment = paymentRepo.findByStripePaymentIntentId(paymentIntentId).orElse(null);

        TimeSlot paymentSlot = lock != null ? lock.getSlot() : payment != null ? payment.getSlot() : null;
        User user = lock != null ? lock.getUser() : payment != null ? payment.getUser() : null;

        if (paymentSlot == null || user == null) {
            throw new IllegalStateException("Cannot resolve paid booking context for paymentIntentId=" + paymentIntentId);
        }

        TimeSlot slot = slotRepo.findByIdWithLock(paymentSlot.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found: " + paymentSlot.getId()));
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = lock != null ? lock.getExpiresAt() : payment != null ? payment.getExpiresAt() : null;

        if (payment != null && isTerminalWithoutBooking(payment.getStatus())) {
            refundAndMark(payment, paymentIntentId, slot, user, expiresAt,
                    "Payment succeeded after it was already " + payment.getStatus());
            releaseLockIfPresent(lock, slot);
            return;
        }

        // 1. Check if the slot already has a confirmed booking
        if (slot.getStatus() == Status.BOOKED || bookingRepo.existsBySlotIdAndStatus(slot.getId(), BookingStatus.CONFIRMED)) {
            refundAndMark(payment, paymentIntentId, slot, user, expiresAt, "Slot already has another booking");
            releaseLockIfPresent(lock, slot);
            return;
        }

        // 2. If the lock has expired, check if another user has locked this slot in the meantime
        if (expiresAt != null && expiresAt.isBefore(now)) {
            boolean lockedBySomeoneElse = seatLockRepo.findBySlot_Id(slot.getId())
                    .map(activeLock -> !activeLock.getUser().getId().equals(user.getId()))
                    .orElse(false);

            if (lockedBySomeoneElse) {
                refundAndMark(payment, paymentIntentId, slot, user, expiresAt, "Lock expired and slot was locked by another user");
                releaseLockIfPresent(lock, slot);
                return;
            }
            log.info("Lock expired but slot is still available or held by the same user. Confirming booking for slotId={} paymentIntentId={}", slot.getId(), paymentIntentId);
        }

        slot.setStatus(Status.BOOKED);
        slotRepo.save(slot);

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setSlot(slot);
        booking.setPaymentIntentId(paymentIntentId);
        booking.setAmountPaid(payment != null ? payment.getAmount() : slot.getExpert().getSessionPrice());
        booking.setCurrency(payment != null ? payment.getCurrency() : slot.getExpert().getCurrency());
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setBookedAt(LocalDateTime.now());
        bookingRepo.save(booking);

        upsertPaymentAudit(booking, paymentIntentId, slot, user, expiresAt, PaymentStatus.SUCCEEDED);

        if (lock != null) {
            seatLockRepo.delete(lock);
        }
        notificationService.broadcastSlotUpdate(slot, null);

        log.info("Booking CONFIRMED - bookingId={} slotId={} userId={} amount={} {}",
                booking.getId(), slot.getId(), user.getId(),
                booking.getAmountPaid(), booking.getCurrency());
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
                || status == PaymentStatus.REFUNDED;
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
