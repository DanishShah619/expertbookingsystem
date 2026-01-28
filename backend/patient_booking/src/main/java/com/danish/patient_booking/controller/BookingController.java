package com.danish.patient_booking.controller;

import com.danish.patient_booking.dto.BookingDto;
import com.danish.patient_booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    // User: get my bookings
    @GetMapping("/bookings/me")
    public ResponseEntity<List<BookingDto>> getMyBookings(
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
                bookingService.getMyBookings(jwt.getSubject())
        );
    }

    // User: cancel a booking
    @DeleteMapping("/bookings/{id}")
    public ResponseEntity<Void> cancelBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        bookingService.cancelBooking(id, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    // Admin: get all bookings
    @GetMapping("/admin/bookings")
    public ResponseEntity<List<BookingDto>> getAllBookings() {
        return ResponseEntity.ok(bookingService.getAllBookings());
    }
}