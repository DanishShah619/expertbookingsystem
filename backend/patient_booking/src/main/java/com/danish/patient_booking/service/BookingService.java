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

    @Transactional
    public void confirmBooking(String paymentIntentId) {
        if (bookingRepo.existsByPaymentIntentId(paymentIntentId)) {
            log.info("Booking already exists for paymentIntentId={} - skipping duplicate webhook", paymentIntentId);
            return;
        }

        SeatLock lock = seatLockRepo.findByPaymentIntentId(paymentIntentId).orElse(null);
        if (lock == null) {
            log.warn("No active SeatLock found for paymentIntentId={} - already expired or processed", paymentIntentId);
            return;
        }

        TimeSlot slot = lock.getSlot();
        if (lock.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Lock expired before webhook arrived for paymentIntentId={} - refunding", paymentIntentId);
            try {
                stripeService.refundPaymentIntent(paymentIntentId);
                upsertPaymentAudit(null, paymentIntentId, slot, PaymentStatus.REFUNDED);
            } catch (Exception e) {
                log.error("Refund failed for expired lock paymentIntentId={}: {}", paymentIntentId, e.getMessage());
            }
            if (slot.getStatus() != Status.BOOKED) {
                slot.setStatus(Status.AVAILABLE);
                slotRepo.save(slot);
                notificationService.broadcastSlotUpdate(slot, null);
            }
            seatLockRepo.delete(lock);
            return;
        }

        User user = lock.getUser();

        slot.setStatus(Status.BOOKED);
        slotRepo.save(slot);

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setSlot(slot);
        booking.setPaymentIntentId(paymentIntentId);
        booking.setAmountPaid(slot.getExpert().getSessionPrice());
        booking.setCurrency(slot.getExpert().getCurrency());
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setBookedAt(LocalDateTime.now());
        bookingRepo.save(booking);

        upsertPaymentAudit(booking, paymentIntentId, slot, PaymentStatus.SUCCEEDED);

        seatLockRepo.delete(lock);
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
                    upsertPaymentAudit(null, paymentIntentId, slot, PaymentStatus.FAILED);
                    seatLockRepo.delete(lock);
                    notificationService.broadcastSlotUpdate(slot, null);
                    log.info("Payment failed - slotId={} released to AVAILABLE", slot.getId());
                },
                () -> log.warn("Payment failure webhook: no lock found for paymentIntentId={}", paymentIntentId)
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

        TimeSlot slot = booking.getSlot();
        slot.setStatus(Status.AVAILABLE);
        slotRepo.save(slot);
        notificationService.broadcastSlotUpdate(slot, null);

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
                                    PaymentStatus status) {
        Payment payment = paymentRepo.findByStripePaymentIntentId(paymentIntentId)
                .orElseGet(Payment::new);

        payment.setBooking(booking);
        payment.setStripePaymentIntentId(paymentIntentId);
        payment.setAmount(slot.getExpert().getSessionPrice());
        payment.setCurrency(slot.getExpert().getCurrency());
        payment.setStatus(status);
        payment.setUpdatedAt(LocalDateTime.now());

        if (payment.getCreatedAt() == null) {
            payment.setCreatedAt(LocalDateTime.now());
        }

        paymentRepo.save(payment);
    }
}
