package com.danish.patient_booking.service;

import com.danish.patient_booking.dto.*;
import com.danish.patient_booking.model.Expert;
import com.danish.patient_booking.model.SeatLock;
import com.danish.patient_booking.model.TimeSlot;
import com.danish.patient_booking.exception.ResourceNotFoundException;
import com.danish.patient_booking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpertService {

    private final ExpertRepository   expertRepo;
    private final TimeSlotRepository slotRepo;
    private final SeatLockRepository seatLockRepo;

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC — any authenticated user
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ExpertDto> getAllExperts() {
        return expertRepo.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExpertDto getExpertById(Long id) {
        return toDto(findExpertOrThrow(id));
    }

    /**
     * Returns slots for an expert with their current status.
     * For LOCKED slots, also includes lockExpiresAt so the
     * frontend can render the countdown timer.
     */
    @Transactional(readOnly = true)
    public List<TimeSlotDto> getSlotsByExpert(Long expertId) {

        // Verify expert exists first
        findExpertOrThrow(expertId);

        List<TimeSlot> slots = slotRepo.findByExpertId(expertId);

        // Fetch all active locks for these slots in ONE query
        // instead of N queries (one per slot) — avoids N+1 problem
        List<Long> slotIds = slots.stream()
                .map(TimeSlot::getId)
                .collect(Collectors.toList());

        Map<Long, LocalDateTime> lockExpiryBySlotId =
                seatLockRepo.findAllBySlotIdIn(slotIds)
                        .stream()
                        .collect(Collectors.toMap(
                                lock -> lock.getSlot().getId(),
                                SeatLock::getExpiresAt
                        ));

        return slots.stream()
                .map(slot -> toSlotDto(slot, lockExpiryBySlotId.get(slot.getId())))
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN — create / update / delete experts
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public ExpertDto createExpert(ExpertCreateRequest req) {
        Expert expert = Expert.builder()
                .name(req.getName())
                .title(req.getTitle())
                .bio(req.getBio())
                .photoUrl(req.getPhotoUrl())
                .tags(req.getTags())
                .sessionPrice(req.getSessionPrice())
                .currency(req.getCurrency().toUpperCase())
                .build();

        Expert saved = expertRepo.save(expert);
        log.info("Expert created: id={} name={}", saved.getId(), saved.getName());
        return toDto(saved);
    }

    @Transactional
    public ExpertDto updateExpert(Long id, ExpertCreateRequest req) {
        Expert expert = findExpertOrThrow(id);

        expert.setName(req.getName());
        expert.setTitle(req.getTitle());
        expert.setBio(req.getBio());
        expert.setPhotoUrl(req.getPhotoUrl());
        expert.setTags(req.getTags());
        expert.setSessionPrice(req.getSessionPrice());
        expert.setCurrency(req.getCurrency().toUpperCase());

        Expert saved = expertRepo.save(expert);
        log.info("Expert updated: id={}", saved.getId());
        return toDto(saved);
    }

    @Transactional
    public void deleteExpert(Long id) {
        Expert expert = findExpertOrThrow(id);
        expertRepo.delete(expert);
        log.info("Expert deleted: id={}", id);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN — create / delete slots
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public TimeSlotDto createSlot(SlotCreateRequest req) {

        Expert expert = findExpertOrThrow(req.getExpertId());

        // Guard: end time must be after start time
        if (!req.getEndTime().isAfter(req.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        TimeSlot slot = TimeSlot.builder()
                .expert(expert)
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .build();

        TimeSlot saved = slotRepo.save(slot);
        log.info("Slot created: id={} expertId={}", saved.getId(), expert.getId());
        return toSlotDto(saved, null);
    }

    @Transactional
    public void deleteSlot(Long slotId) {

        TimeSlot slot = slotRepo.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found: " + slotId));

        // Guard: don't delete a slot that is locked or booked
        // — someone is mid-payment or already confirmed
        if (slot.getStatus() != com.danish.patient_booking.enums.SlotStatus.AVAILABLE) {
            throw new IllegalStateException(
                    "Cannot delete slot with status: " + slot.getStatus()
                            + " — only AVAILABLE slots can be deleted"
            );
        }

        slotRepo.delete(slot);
        log.info("Slot deleted: id={}", slotId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private Expert findExpertOrThrow(Long id) {
        return expertRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expert not found: " + id));
    }

    private ExpertDto toDto(Expert expert) {
        return ExpertDto.builder()
                .id(expert.getId())
                .name(expert.getName())
                .title(expert.getTitle())
                .bio(expert.getBio())
                .photoUrl(expert.getPhotoUrl())
                .tags(expert.getTags())
                .sessionPrice(expert.getSessionPrice())
                .currency(expert.getCurrency())
                .createdAt(expert.getCreatedAt())
                .build();
    }

    private TimeSlotDto toSlotDto(TimeSlot slot, LocalDateTime lockExpiresAt) {
        return TimeSlotDto.builder()
                .id(slot.getId())
                .expertId(slot.getExpert().getId())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .status(slot.getStatus())
                .lockExpiresAt(lockExpiresAt)
                .build();
    }
}
