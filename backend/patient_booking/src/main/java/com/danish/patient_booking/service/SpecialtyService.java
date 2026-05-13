package com.danish.patient_booking.service;


import com.danish.patient_booking.dto.SpecialtyDto;
import com.danish.patient_booking.model.Specialty;
import com.danish.patient_booking.exception.ResourceNotFoundException;
import com.danish.patient_booking.repository.SpecialtyRepository;
import lombok.RequiredArgsConstructor;
import com.danish.patient_booking.util.AppLogger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SpecialtyService {

    private static final AppLogger log = AppLogger.getLogger(SpecialtyService.class);

    private final SpecialtyRepository specialtyRepo;

    @Transactional(readOnly = true)
    public List<SpecialtyDto> getAllSpecialties() {
        return specialtyRepo.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public SpecialtyDto createSpecialty(String name) {

        if (specialtyRepo.existsByName(name)) {
            throw new IllegalArgumentException("Specialty already exists: " + name);
        }

        Specialty specialty = Specialty.builder()
                .name(name)
                .slug(toSlug(name))    // "ENT & Head/Neck" → "ent-head-neck"
                .build();

        Specialty saved = specialtyRepo.save(specialty);
        log.info("Specialty created: id={} name={}", saved.getId(), saved.getName());
        return toDto(saved);
    }

    @Transactional
    public void deleteSpecialty(Long id) {
        Specialty specialty = specialtyRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Specialty not found: " + id));
        specialtyRepo.delete(specialty);
    }

    public Specialty findByIdOrThrow(Long id) {
        return specialtyRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Specialty not found: " + id));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private String toSlug(String name) {
        return name.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "")  // remove special chars
                .replaceAll("\\s+", "-");           // spaces → hyphens
        // "ENT & Head Neck" → "ent  head neck" → "ent-head-neck"
    }

    private SpecialtyDto toDto(Specialty s) {
        return SpecialtyDto.builder()
                .id(s.getId())
                .name(s.getName())
                .slug(s.getSlug())
                .build();
    }
}