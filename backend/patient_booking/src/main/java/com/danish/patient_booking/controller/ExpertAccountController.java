package com.danish.patient_booking.controller;


import com.danish.patient_booking.dto.ExpertBookingDto;
import com.danish.patient_booking.dto.ExpertProfileDto;
import com.danish.patient_booking.service.ExpertAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expert")
@RequiredArgsConstructor
@PreAuthorize("hasRole('EXPERT')")    // only EXPERT role can access these
public class ExpertAccountController {

    private final ExpertAccountService expertAccountService;

    /**
     * GET /api/expert/me
     * Expert's own profile + dashboard stats
     * (total bookings, upcoming, completed)
     */
    @GetMapping("/me")
    public ResponseEntity<ExpertProfileDto> getMyProfile(
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
                expertAccountService.getMyProfile(jwt.getSubject())
        );
    }

    /**
     * GET /api/expert/bookings
     * All bookings for this expert's slots — with patient info
     */
    @GetMapping("/bookings")
    public ResponseEntity<List<ExpertBookingDto>> getMyBookings(
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
                expertAccountService.getMyBookings(jwt.getSubject())
        );
    }

    /**
     * GET /api/expert/bookings/upcoming
     * Only future confirmed bookings — for "upcoming sessions" tab
     */
    @GetMapping("/bookings/upcoming")
    public ResponseEntity<List<ExpertBookingDto>> getUpcoming(
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
                expertAccountService.getUpcomingBookings(jwt.getSubject())
        );
    }

    /**
     * GET /api/expert/bookings/today
     * Today's schedule — for daily view on dashboard
     */
    @GetMapping("/bookings/today")
    public ResponseEntity<List<ExpertBookingDto>> getToday(
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
                expertAccountService.getTodaysBookings(jwt.getSubject())
        );
    }
}