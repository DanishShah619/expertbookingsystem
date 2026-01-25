package com.danish.patient_booking.dto;

import com.danish.patient_booking.enums.Role;
import com.danish.patient_booking.model.User;

public record UserDto(
        Long id,
        String googleId,
        String email,
        String name,
        String pictureUrl,
        Role role,
        Long expertId
) {
    public static UserDto from(User user) {
        Long expertId = user.getExpertProfile() == null ? null : user.getExpertProfile().getId();
        return new UserDto(
                user.getId(),
                user.getGoogleId(),
                user.getEmail(),
                user.getName(),
                user.getPictureUrl(),
                user.getRole(),
                expertId
        );
    }
}
