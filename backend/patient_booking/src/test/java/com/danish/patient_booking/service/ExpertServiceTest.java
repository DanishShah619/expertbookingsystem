package com.danish.patient_booking.service;

import com.danish.patient_booking.dto.SlotCreateRequest;
import com.danish.patient_booking.enums.Role;
import com.danish.patient_booking.exception.SlotNotAvailableException;
import com.danish.patient_booking.model.Expert;
import com.danish.patient_booking.model.User;
import com.danish.patient_booking.repository.ExpertRepository;
import com.danish.patient_booking.repository.SeatLockRepository;
import com.danish.patient_booking.repository.TimeSlotRepository;
import com.danish.patient_booking.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpertServiceTest {

    @Mock
    private SpecialtyService specialtyService;
    @Mock
    private UserRepository userRepo;
    @Mock
    private ExpertRepository expertRepo;
    @Mock
    private TimeSlotRepository slotRepo;
    @Mock
    private SeatLockRepository seatLockRepo;

    @InjectMocks
    private ExpertService expertService;

    @Test
    void createSlotRejectsOverlappingSlot() {
        Expert expert = new Expert();
        expert.setId(7L);
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        SlotCreateRequest request = new SlotCreateRequest();
        request.setExpertId(7L);
        request.setStartTime(start);
        request.setEndTime(start.plusHours(1));

        when(expertRepo.findById(7L)).thenReturn(Optional.of(expert));
        when(slotRepo.existsOverlappingSlot(7L, request.getStartTime(), request.getEndTime())).thenReturn(true);

        assertThatThrownBy(() -> expertService.createSlot(request))
                .isInstanceOf(SlotNotAvailableException.class)
                .hasMessageContaining("overlaps");

        verify(slotRepo, never()).save(any());
    }

    @Test
    void deleteExpertRejectsExpertWithSlots() {
        Expert expert = new Expert();
        expert.setId(8L);

        when(expertRepo.findById(8L)).thenReturn(Optional.of(expert));
        when(slotRepo.existsByExpertId(8L)).thenReturn(true);

        assertThatThrownBy(() -> expertService.deleteExpert(8L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("existing slots");

        verify(expertRepo, never()).delete(any());
    }

    @Test
    void deleteExpertDemotesLinkedExpertUserWhenNoSlotsRemain() {
        User linkedUser = new User();
        linkedUser.setId(9L);
        linkedUser.setRole(Role.EXPERT);
        Expert expert = new Expert();
        expert.setId(9L);
        expert.setUser(linkedUser);

        when(expertRepo.findById(9L)).thenReturn(Optional.of(expert));
        when(slotRepo.existsByExpertId(9L)).thenReturn(false);

        expertService.deleteExpert(9L);

        verify(expertRepo).delete(expert);
        verify(userRepo).save(linkedUser);
    }
}
