package com.danish.patient_booking.dto;


import com.danish.patient_booking.enums.Status;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class SlotUpdateEvent {
    private Long          slotId;
    private Long          expertId;
    private Status status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    /**
     * Only populated when status = LOCKED.
     * Frontend uses this to render the "held for X:XX" countdown
     * on the slot grid for other users watching the same expert's page.
     * Null for AVAILABLE and BOOKED.
     */
    private LocalDateTime lockExpiresAt;
}