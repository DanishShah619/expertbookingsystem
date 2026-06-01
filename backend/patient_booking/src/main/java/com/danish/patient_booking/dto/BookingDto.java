package com.danish.patient_booking.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class BookingDto {
    private Long          id;
    private Long          slotId;
    private Long          expertId;
    private String        expertName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal    amountPaid;
    private String        currency;
    private String        status;
    private LocalDateTime bookedAt;
    private String        paymentIntentId;
}