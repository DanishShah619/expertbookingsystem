package com.danish.patient_booking.repository;

import com.danish.patient_booking.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


// BookingRepository.java
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Idempotency check in confirmBooking()
    boolean existsByPaymentIntentId(String paymentIntentId);

    // Get user's own bookings newest first
    List<Booking> findByUserIdOrderByBookedAtDesc(Long userId);

    // Admin: all bookings newest first
    List<Booking> findAllByOrderByBookedAtDesc();
}

