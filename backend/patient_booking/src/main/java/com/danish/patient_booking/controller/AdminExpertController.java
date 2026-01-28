package com.danish.patient_booking.controller;


import com.danish.patient_booking.dto.ExpertCreateRequest;
import com.danish.patient_booking.dto.ExpertDto;
import com.danish.patient_booking.service.ExpertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/experts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")   // applies to every method in this class
public class AdminExpertController {

    private final ExpertService expertService;

    @GetMapping
    public ResponseEntity<List<ExpertDto>> getAllExperts() {
        return ResponseEntity.ok(expertService.getAllExperts());
    }

    @PostMapping
    public ResponseEntity<ExpertDto> createExpert(
            @Valid @RequestBody ExpertCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(expertService.createExpert(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExpertDto> updateExpert(
            @PathVariable Long id,
            @Valid @RequestBody ExpertCreateRequest req) {
        return ResponseEntity.ok(expertService.updateExpert(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpert(@PathVariable Long id) {
        expertService.deleteExpert(id);
        return ResponseEntity.noContent().build();
    }
}
