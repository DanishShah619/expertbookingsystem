package com.danish.patient_booking.controller;

import com.danish.patient_booking.dto.ExpertDto;
import com.danish.patient_booking.dto.SpecialtyDto;
import com.danish.patient_booking.dto.TimeSlotDto;
import com.danish.patient_booking.service.ExpertService;
import com.danish.patient_booking.service.SpecialtyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/experts")
@RequiredArgsConstructor
public class ExpertController {

    private final ExpertService expertService;
    private final SpecialtyService specialtyService;

    @GetMapping
    public ResponseEntity<List<ExpertDto>> getExperts(
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) String search) {

        if (specialty != null && !specialty.isBlank()) {
            return ResponseEntity.ok(expertService.getExpertsBySpecialty(specialty));
        }
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(expertService.searchExperts(search));
        }
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
    @GetMapping("/specialties")
    public ResponseEntity<List<SpecialtyDto>> getSpecialties() {
        return ResponseEntity.ok(specialtyService.getAllSpecialties());
    }
}