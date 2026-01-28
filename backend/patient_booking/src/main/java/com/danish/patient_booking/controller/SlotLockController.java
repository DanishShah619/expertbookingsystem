package com.danish.patient_booking.controller;

import com.danish.patient_booking.dto.SlotLockResponse;
import com.danish.patient_booking.service.SeatLockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/slots")
@RequiredArgsConstructor
public class SlotLockController {

    private final SeatLockService seatLockService;

    /**
     * Lock a slot — returns Stripe clientSecret for frontend Payment Element
     */
    @PostMapping("/{slotId}/lock")
    public ResponseEntity<SlotLockResponse> lockSlot(
            @PathVariable Long slotId,
            @AuthenticationPrincipal Jwt jwt) {

        SlotLockResponse response = seatLockService.lockSlot(slotId, jwt.getSubject());
        return ResponseEntity.ok(response);
    }

    /**
     * Release a lock manually — user clicked cancel before paying
     */
    @DeleteMapping("/{slotId}/lock")
    public ResponseEntity<Void> releaseLock(
            @PathVariable Long slotId,
            @AuthenticationPrincipal Jwt jwt) {

        seatLockService.releaseLock(slotId, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }
}
