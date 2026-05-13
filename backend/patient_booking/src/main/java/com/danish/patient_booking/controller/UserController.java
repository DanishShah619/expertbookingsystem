package com.danish.patient_booking.controller;

import com.danish.patient_booking.dto.BookingDto;
import com.danish.patient_booking.dto.UserProfileDto;
import com.danish.patient_booking.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> me(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(userService.getProfile(jwt.getSubject()));
    }

    @GetMapping("/bookings")
    public ResponseEntity<List<BookingDto>> bookings(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(userService.getBookings(jwt.getSubject()));
    }

    @GetMapping("/bookings/upcoming")
    public ResponseEntity<List<BookingDto>> upcomingBookings(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(userService.getUpcomingBookings(jwt.getSubject()));
    }

    @GetMapping("/bookings/past")
    public ResponseEntity<List<BookingDto>> pastBookings(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(userService.getPastBookings(jwt.getSubject()));
    }
}
