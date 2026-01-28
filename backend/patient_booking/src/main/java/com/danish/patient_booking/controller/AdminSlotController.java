package com.danish.patient_booking.controller;


import com.danish.patient_booking.dto.SlotCreateRequest;
import com.danish.patient_booking.dto.TimeSlotDto;
import com.danish.patient_booking.service.ExpertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/slots")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSlotController {

    private final ExpertService expertService;

    @PostMapping
    public ResponseEntity<TimeSlotDto> createSlot(
            @Valid @RequestBody SlotCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(expertService.createSlot(req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSlot(@PathVariable Long id) {
        expertService.deleteSlot(id);
        return ResponseEntity.noContent().build();
    }
}