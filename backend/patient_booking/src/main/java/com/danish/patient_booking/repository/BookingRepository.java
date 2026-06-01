package com.danish.patient_booking.repository;

import com.danish.patient_booking.model.Booking;
import com.danish.patient_booking.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;


// BookingRepository.java
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Idempotency check in confirmBooking()
    boolean existsByPaymentIntentId(String paymentIntentId);

    boolean existsBySlotIdAndStatus(Long slotId, BookingStatus status);

    // Get user's own bookings newest first
    List<Booking> findByUserIdOrderByBookedAtDesc(Long userId);

    @Query("""
        SELECT b FROM Booking b
        WHERE b.user.id = :userId
        AND b.slot.startTime > :now
        AND b.status = com.danish.patient_booking.enums.BookingStatus.CONFIRMED
        ORDER BY b.slot.startTime ASC
    """)
    List<Booking> findUpcomingByUserId(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now
    );

    @Query("""
        SELECT b FROM Booking b
        WHERE b.user.id = :userId
        AND b.slot.startTime <= :now
        ORDER BY b.slot.startTime DESC
    """)
    List<Booking> findPastByUserId(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now
    );

    // Admin: all bookings newest first
    List<Booking> findAllByOrderByBookedAtDesc();

    List<Booking> findBySlotExpertIdOrderBySlotStartTimeDesc(Long expertId);

    // Only upcoming confirmed bookings for expert
    @Query("""
        SELECT b FROM Booking b
        WHERE b.slot.expert.id = :expertId
        AND b.slot.startTime > :now
        AND b.status = com.danish.patient_booking.enums.BookingStatus.CONFIRMED
        ORDER BY b.slot.startTime ASC
    """)
    List<Booking> findUpcomingByExpertId(
            @Param("expertId") Long expertId,
            @Param("now") LocalDateTime now
    );

    // Today's bookings for expert — between start and end of today
    List<Booking> findBySlotExpertIdAndSlotStartTimeBetweenOrderBySlotStartTime(
            Long expertId,
            LocalDateTime start,
            LocalDateTime end
    );
}

