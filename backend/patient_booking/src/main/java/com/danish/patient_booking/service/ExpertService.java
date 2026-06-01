package com.danish.patient_booking.service;

import com.danish.patient_booking.dto.ExpertCreateRequest;
import com.danish.patient_booking.dto.ExpertDto;
import com.danish.patient_booking.dto.SlotCreateRequest;
import com.danish.patient_booking.dto.SpecialtyDto;
import com.danish.patient_booking.dto.TimeSlotDto;
import com.danish.patient_booking.enums.Role;
import com.danish.patient_booking.enums.Status;
import com.danish.patient_booking.exception.ResourceNotFoundException;
import com.danish.patient_booking.exception.SlotNotAvailableException;
import com.danish.patient_booking.model.Expert;
import com.danish.patient_booking.model.SeatLock;
import com.danish.patient_booking.model.Specialty;
import com.danish.patient_booking.model.TimeSlot;
import com.danish.patient_booking.model.User;
import com.danish.patient_booking.repository.ExpertRepository;
import com.danish.patient_booking.repository.SeatLockRepository;
import com.danish.patient_booking.repository.TimeSlotRepository;
import com.danish.patient_booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import com.danish.patient_booking.util.AppLogger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpertService {

    private static final AppLogger log = AppLogger.getLogger(ExpertService.class);

    private final SpecialtyService specialtyService;
    private final UserRepository userRepo;
    private final ExpertRepository expertRepo;
    private final TimeSlotRepository slotRepo;
    private final SeatLockRepository seatLockRepo;

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

    @Transactional(readOnly = true)
    public List<TimeSlotDto> getSlotsByExpert(Long expertId) {
        findExpertOrThrow(expertId);

        List<TimeSlot> slots = slotRepo.findByExpertIdOrderByStartTimeAsc(expertId);
        List<Long> slotIds = slots.stream()
                .map(TimeSlot::getId)
                .collect(Collectors.toList());

        Map<Long, LocalDateTime> lockExpiryBySlotId = slotIds.isEmpty()
                ? Map.of()
                : seatLockRepo.findAllBySlotIdIn(slotIds)
                        .stream()
                        .collect(Collectors.toMap(
                                lock -> lock.getSlot().getId(),
                                SeatLock::getExpiresAt
                        ));

        return slots.stream()
                .map(slot -> toSlotDto(slot, lockExpiryBySlotId.get(slot.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ExpertDto> getExpertsBySpecialty(String slug) {
        return expertRepo.findBySpecialtySlug(slug)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ExpertDto> searchExperts(String query) {
        if (query == null || query.isBlank()) {
            return getAllExperts();
        }
        return expertRepo.searchExperts(query.trim())
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ExpertDto createExpert(ExpertCreateRequest req) {
        Specialty specialty = specialtyService.findByIdOrThrow(req.getSpecialtyId());
        User user = findExpertUser(req.getUserId());

        user.setRole(Role.EXPERT);
        userRepo.save(user);

        Expert expert = Expert.builder()
                .name(req.getName())
                .title(req.getTitle())
                .bio(req.getBio())
                .photoUrl(req.getPhotoUrl())
                .specialty(specialty)
                .tags(req.getTags())
                .sessionPrice(req.getSessionPrice())
                .currency(req.getCurrency().toUpperCase())
                .user(user)
                .build();

        Expert saved = expertRepo.save(expert);
        log.info("Expert created: id={} linkedUserId={}", saved.getId(), user.getId());
        return toDto(saved);
    }

    @Transactional
    public ExpertDto updateExpert(Long id, ExpertCreateRequest req) {
        Expert expert = findExpertOrThrow(id);
        Specialty specialty = specialtyService.findByIdOrThrow(req.getSpecialtyId());
        User user = findExpertUser(req.getUserId());
        User previousUser = expert.getUser();

        user.setRole(Role.EXPERT);
        userRepo.save(user);

        expert.setName(req.getName());
        expert.setTitle(req.getTitle());
        expert.setBio(req.getBio());
        expert.setPhotoUrl(req.getPhotoUrl());
        expert.setSpecialty(specialty);
        expert.setTags(req.getTags());
        expert.setSessionPrice(req.getSessionPrice());
        expert.setCurrency(req.getCurrency().toUpperCase());
        expert.setUser(user);

        Expert saved = expertRepo.save(expert);
        demotePreviousExpertUser(previousUser, user);
        log.info("Expert updated: id={} linkedUserId={}", saved.getId(), user.getId());
        return toDto(saved);
    }

    @Transactional
    public void deleteExpert(Long id) {
        Expert expert = findExpertOrThrow(id);
        if (slotRepo.existsByExpertId(id)) {
            throw new IllegalStateException("Cannot delete expert with existing slots. Delete future empty slots or deactivate the expert instead.");
        }

        User linkedUser = expert.getUser();
        expertRepo.delete(expert);
        demotePreviousExpertUser(linkedUser, null);
        log.info("Expert deleted: id={}", id);
    }

    @Transactional
    public TimeSlotDto createSlot(SlotCreateRequest req) {
        Expert expert = findExpertOrThrow(req.getExpertId());

        if (!req.getEndTime().isAfter(req.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        if (slotRepo.existsOverlappingSlot(expert.getId(), req.getStartTime(), req.getEndTime())) {
            throw new SlotNotAvailableException("Slot overlaps an existing slot for this expert");
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

        if (slot.getStatus() != Status.AVAILABLE) {
            throw new IllegalStateException(
                    "Cannot delete slot with status: " + slot.getStatus()
                            + " - only AVAILABLE slots can be deleted"
            );
        }

        slotRepo.delete(slot);
        log.info("Slot deleted: id={}", slotId);
    }

    private Expert findExpertOrThrow(Long id) {
        return expertRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expert not found: " + id));
    }

    private User findExpertUser(Long userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + userId
                                + " - user must sign in with Google at least once before being made an expert"
                ));
    }

    private void demotePreviousExpertUser(User previousUser, User currentUser) {
        if (previousUser == null) {
            return;
        }
        if (currentUser != null && previousUser.getId().equals(currentUser.getId())) {
            return;
        }
        if (previousUser.getRole() == Role.EXPERT) {
            previousUser.setRole(Role.USER);
            userRepo.save(previousUser);
        }
    }

    private ExpertDto toDto(Expert expert) {
        return ExpertDto.builder()
                .id(expert.getId())
                .name(expert.getName())
                .title(expert.getTitle())
                .bio(expert.getBio())
                .photoUrl(expert.getPhotoUrl())
                .specialty(SpecialtyDto.builder()
                        .id(expert.getSpecialty().getId())
                        .name(expert.getSpecialty().getName())
                        .slug(expert.getSpecialty().getSlug())
                        .build())
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
                .lockExpiresAt(lockExpiresAt != null ? lockExpiresAt.atZone(java.time.ZoneId.systemDefault()).toInstant() : null)
                .build();
    }
}
