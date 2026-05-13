package com.danish.patient_booking.controller;

import com.danish.patient_booking.dto.SpecialtyDto;
import com.danish.patient_booking.service.SpecialtyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/specialties")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSpecialtyController {

    private final SpecialtyService specialtyService;

    @GetMapping
    public ResponseEntity<List<SpecialtyDto>> getAll() {
        return ResponseEntity.ok(specialtyService.getAllSpecialties());
    }

    @PostMapping
    public ResponseEntity<SpecialtyDto> create(@RequestParam String name) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(specialtyService.createSpecialty(name));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        specialtyService.deleteSpecialty(id);
        return ResponseEntity.noContent().build();
    }
}