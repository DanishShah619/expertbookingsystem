package com.danish.patient_booking.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ExpertBookingDto {
    private Long          bookingId;
    private Long          slotId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // Patient info — expert needs to know who is coming
    private String        patientName;
    private String        patientEmail;

    private BigDecimal    amountPaid;
    private String        currency;
    private String        status;
    private LocalDateTime bookedAt;

    // Derived — useful for expert dashboard grouping
    private boolean       isUpcoming;   // startTime is in the future
    private boolean       isToday;      // startTime is today
}
