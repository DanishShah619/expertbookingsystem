package com.danish.patient_booking.controller;

import com.danish.patient_booking.dto.ExpertDto;
import com.danish.patient_booking.dto.TimeSlotDto;
import com.danish.patient_booking.service.ExpertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/experts")
@RequiredArgsConstructor
public class ExpertController {

    private final ExpertService expertService;

    @GetMapping
    public ResponseEntity<List<ExpertDto>> getAllExperts() {
        return ResponseEntity.ok(expertService.getAllExperts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpertDto> getExpert(@PathVariable Long id) {
        return ResponseEntity.ok(expertService.getExpertById(id));
    }

    // Returns slots with live status + lockExpiresAt for LOCKED slots
    @GetMapping("/{id}/slots")
    public ResponseEntity<List<TimeSlotDto>> getSlots(@PathVariable Long id) {
        return ResponseEntity.ok(expertService.getSlotsByExpert(id));
    }
}