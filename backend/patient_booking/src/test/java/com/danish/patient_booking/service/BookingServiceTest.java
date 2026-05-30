package com.danish.patient_booking.service;

import com.danish.patient_booking.enums.BookingStatus;
import com.danish.patient_booking.enums.Currency;
import com.danish.patient_booking.enums.PaymentStatus;
import com.danish.patient_booking.enums.Status;
import com.danish.patient_booking.exception.SlotNotAvailableException;
import com.danish.patient_booking.model.Booking;
import com.danish.patient_booking.model.Expert;
import com.danish.patient_booking.model.Payment;
import com.danish.patient_booking.model.TimeSlot;
import com.danish.patient_booking.model.User;
import com.danish.patient_booking.repository.BookingRepository;
import com.danish.patient_booking.repository.PaymentRepository;
import com.danish.patient_booking.repository.SeatLockRepository;
import com.danish.patient_booking.repository.TimeSlotRepository;
import com.danish.patient_booking.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepo;
    @Mock
    private SeatLockRepository seatLockRepo;
    @Mock
    private TimeSlotRepository slotRepo;
    @Mock
    private PaymentRepository paymentRepo;
    @Mock
    private StripeService stripeService;
    @Mock
    private WebSocketNotificationService notificationService;
    @Mock
    private UserRepository userRepo;

    @InjectMocks
    private BookingService bookingService;

    @Test
    void cancelBookingRejectsPastSessionsBeforeRefunding() throws Exception {
        ReflectionTestUtils.setField(bookingService, "cancelCutoffMinutes", 60L);
        User user = user(10L);
        TimeSlot slot = slot(20L, Status.BOOKED, LocalDateTime.now().minusHours(2));
        Booking booking = new Booking();
        booking.setId(30L);
        booking.setUser(user);
        booking.setSlot(slot);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentIntentId("pi_past");

        when(userRepo.findByGoogleId("google-10")).thenReturn(Optional.of(user));
        when(bookingRepo.findById(30L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking(30L, "google-10"))
                .isInstanceOf(SlotNotAvailableException.class)
                .hasMessageContaining("cancelled at least 60 minutes");

        verify(stripeService, never()).refundPaymentIntent(any());
        verify(slotRepo, never()).save(any());
    }

    @Test
    void confirmBookingRefundsExpiredPaymentWithoutCreatingBooking() throws Exception {
        User user = user(11L);
        TimeSlot slot = slot(21L, Status.AVAILABLE, LocalDateTime.now().plusDays(1));
        Payment payment = new Payment();
        payment.setUser(user);
        payment.setSlot(slot);
        payment.setStripePaymentIntentId("pi_expired");
        payment.setAmount(slot.getExpert().getSessionPrice());
        payment.setCurrency(Currency.INR.name());
        payment.setStatus(PaymentStatus.EXPIRED);
        payment.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(bookingRepo.existsByPaymentIntentId("pi_expired")).thenReturn(false);
        when(seatLockRepo.findByPaymentIntentId("pi_expired")).thenReturn(Optional.empty());
        when(paymentRepo.findByStripePaymentIntentId("pi_expired")).thenReturn(Optional.of(payment));
        when(slotRepo.findByIdWithLock(21L)).thenReturn(Optional.of(slot));

        bookingService.confirmBooking("pi_expired");

        verify(stripeService).refundPaymentIntent("pi_expired");
        verify(bookingRepo, never()).save(any());
        verify(paymentRepo).save(payment);
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setGoogleId("google-" + id);
        user.setEmail("user" + id + "@example.com");
        return user;
    }

    private TimeSlot slot(Long id, Status status, LocalDateTime startTime) {
        Expert expert = new Expert();
        expert.setId(100L);
        expert.setName("Expert");
        expert.setSessionPrice(BigDecimal.valueOf(500));
        expert.setCurrency(Currency.INR.name());

        TimeSlot slot = new TimeSlot();
        slot.setId(id);
        slot.setExpert(expert);
        slot.setStatus(status);
        slot.setStartTime(startTime);
        slot.setEndTime(startTime.plusHours(1));
        return slot;
    }
}
