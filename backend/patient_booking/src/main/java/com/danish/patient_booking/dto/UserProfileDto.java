package com.danish.patient_booking.dto;

import com.danish.patient_booking.enums.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileDto {
    private Long id;
    private String email;
    private String name;
    private String pictureUrl;
    private Role role;
    private long totalBookings;
    private long upcomingBookings;
    private long completedBookings;
    private long cancelledBookings;
}
