// TimeSlotDto.java
package com.danish.patient_booking.dto;

import com.danish.patient_booking.enums.Status;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Builder
public class TimeSlotDto {
    private Long          id;
    private Long          expertId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Status    status;

    // Only non-null when status = LOCKED
    // Frontend uses this to render the countdown timer
    private Instant lockExpiresAt;
}