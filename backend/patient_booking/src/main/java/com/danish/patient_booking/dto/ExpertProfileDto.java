package com.danish.patient_booking.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ExpertProfileDto {
    private Long         id;
    private String       name;
    private String       title;
    private String       bio;
    private String       photoUrl;
    private SpecialtyDto specialty;
    private String       tags;
    private BigDecimal   sessionPrice;
    private String       currency;
    private LocalDateTime createdAt;

    // Stats shown on expert dashboard
    private long totalBookings;
    private long upcomingBookings;
    private long completedBookings;
}
