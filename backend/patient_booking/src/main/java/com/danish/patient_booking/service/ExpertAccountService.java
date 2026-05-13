package com.danish.patient_booking.service;




import com.danish.patient_booking.dto.ExpertBookingDto;
import com.danish.patient_booking.dto.ExpertProfileDto;
import com.danish.patient_booking.dto.SpecialtyDto;
import com.danish.patient_booking.model.Booking;
import com.danish.patient_booking.model.Expert;
import com.danish.patient_booking.model.User;
import com.danish.patient_booking.exception.ResourceNotFoundException;
import com.danish.patient_booking.repository.BookingRepository;
import com.danish.patient_booking.repository.ExpertRepository;
import com.danish.patient_booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import com.danish.patient_booking.util.AppLogger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpertAccountService {

    private static final AppLogger log = AppLogger.getLogger(ExpertAccountService.class);

    private final ExpertRepository  expertRepo;
    private final BookingRepository bookingRepo;
    private final UserRepository    userRepo;

    // ─────────────────────────────────────────────────────────────────────────
    // GET EXPERT PROFILE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the expert's own profile including dashboard stats.
     * Called on every expert dashboard page load.
     */
    @Transactional(readOnly = true)
    public ExpertProfileDto getMyProfile(String googleId) {
        Expert expert = findExpertByGoogleId(googleId);

        List<Booking> allBookings =
                bookingRepo.findBySlotExpertIdOrderBySlotStartTimeDesc(expert.getId());

        LocalDateTime now = LocalDateTime.now();

        long upcoming = allBookings.stream()
                .filter(b -> b.getSlot().getStartTime().isAfter(now))
                .filter(b -> b.getStatus().name().equals("CONFIRMED"))
                .count();

        long completed = allBookings.stream()
                .filter(b -> b.getSlot().getStartTime().isBefore(now))
                .filter(b -> b.getStatus().name().equals("CONFIRMED"))
                .count();

        return ExpertProfileDto.builder()
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
                .totalBookings(allBookings.size())
                .upcomingBookings(upcoming)
                .completedBookings(completed)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET ALL BOOKINGS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns all bookings for this expert's slots, newest first.
     * Expert sees patient name, email, slot time, and payment info.
     */
    @Transactional(readOnly = true)
    public List<ExpertBookingDto> getMyBookings(String googleId) {
        Expert expert = findExpertByGoogleId(googleId);

        return bookingRepo
                .findBySlotExpertIdOrderBySlotStartTimeDesc(expert.getId())
                .stream()
                .map(this::toExpertBookingDto)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET UPCOMING BOOKINGS ONLY
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns only future confirmed bookings.
     * Used for the expert's "upcoming sessions" tab.
     */
    @Transactional(readOnly = true)
    public List<ExpertBookingDto> getUpcomingBookings(String googleId) {
        Expert expert = findExpertByGoogleId(googleId);

        return bookingRepo
                .findUpcomingByExpertId(expert.getId(), LocalDateTime.now())
                .stream()
                .map(this::toExpertBookingDto)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET TODAY'S BOOKINGS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns bookings for today only.
     * Useful for expert's daily schedule view.
     */
    @Transactional(readOnly = true)
    public List<ExpertBookingDto> getTodaysBookings(String googleId) {
        Expert expert = findExpertByGoogleId(googleId);

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay   = startOfDay.plusDays(1).minusSeconds(1);

        return bookingRepo
                .findBySlotExpertIdAndSlotStartTimeBetweenOrderBySlotStartTime(
                        expert.getId(), startOfDay, endOfDay
                )
                .stream()
                .map(this::toExpertBookingDto)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Finds the Expert entity linked to the logged-in Google user.
     * Throws clearly if user has no expert profile —
     * catches misconfigured accounts early.
     */
    private Expert findExpertByGoogleId(String googleId) {
        User user = userRepo.findByGoogleId(googleId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return expertRepo.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No expert profile linked to this account. " +
                                "Please contact admin to link your profile."
                ));
    }

    private ExpertBookingDto toExpertBookingDto(Booking booking) {
        LocalDateTime now       = LocalDateTime.now();
        LocalDateTime startTime = booking.getSlot().getStartTime();

        return ExpertBookingDto.builder()
                .bookingId(booking.getId())
                .slotId(booking.getSlot().getId())
                .startTime(startTime)
                .endTime(booking.getSlot().getEndTime())
                .patientName(booking.getUser().getName())
                .patientEmail(booking.getUser().getEmail())
                .amountPaid(booking.getAmountPaid())
                .currency(booking.getCurrency())
                .status(booking.getStatus().name())
                .bookedAt(booking.getBookedAt())
                .isUpcoming(startTime.isAfter(now))
                .isToday(startTime.toLocalDate().equals(LocalDate.now()))
                .build();
    }
}