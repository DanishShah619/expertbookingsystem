package com.danish.patient_booking.repository;


import com.danish.patient_booking.model.SeatLock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SeatLockRepository extends JpaRepository<SeatLock, Long> {
    List<SeatLock> findAllByExpiresAtBefore(LocalDateTime now);

    // Used by webhook to find lock after payment
    Optional<SeatLock> findByPaymentIntentId(String paymentIntentId);

    // Used by SeatLockService to prevent duplicate locks
    Optional<SeatLock> findBySlot_IdAndUser_Id(Long slotId, Long userId);

    // Used by releaseLock
    Optional<SeatLock> findBySlot_Id(Long slotId);

    List<SeatLock> findAllBySlotIdIn(List<Long> slotIds);
}