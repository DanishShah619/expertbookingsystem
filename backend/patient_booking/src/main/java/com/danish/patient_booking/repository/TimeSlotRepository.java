package com.danish.patient_booking.repository;


import com.danish.patient_booking.model.TimeSlot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM TimeSlot s WHERE s.id = :id")
    Optional<TimeSlot> findByIdWithLock(@Param("id") Long id);

    List<TimeSlot> findByExpertIdOrderByStartTimeAsc(Long expertId);

    boolean existsByExpertId(Long expertId);

    @Query("""
        SELECT COUNT(s) > 0 FROM TimeSlot s
        WHERE s.expert.id = :expertId
        AND s.startTime < :endTime
        AND s.endTime > :startTime
    """)
    boolean existsOverlappingSlot(@Param("expertId") Long expertId,
                                  @Param("startTime") java.time.LocalDateTime startTime,
                                  @Param("endTime") java.time.LocalDateTime endTime);
}
