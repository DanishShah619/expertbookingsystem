package com.danish.patient_booking.service;

import com.danish.patient_booking.dto.BookingDto;
import com.danish.patient_booking.dto.UserProfileDto;
import com.danish.patient_booking.enums.BookingStatus;
import com.danish.patient_booking.enums.Role;
import com.danish.patient_booking.model.Booking;
import com.danish.patient_booking.model.User;
import com.danish.patient_booking.repository.BookingRepository;
import com.danish.patient_booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    @Transactional
    public User findOrCreateUser(String googleId, String email, String name, String pictureUrl) {
        if (googleId == null || googleId.isBlank()) {
            throw new BadCredentialsException("Missing Google subject");
        }
        if (email == null || email.isBlank()) {
            throw new BadCredentialsException("Missing Google email");
        }

        return userRepository.findByGoogleId(googleId)
                .map(user -> updateProfile(user, name, pictureUrl))
                .orElseGet(() -> userRepository.findByEmail(email)
                        .map(user -> attachGoogleLogin(user, googleId, name, pictureUrl))
                        .orElseGet(() -> createUser(googleId, email, name, pictureUrl)));
    }

    @Transactional(readOnly = true)
    public User findByGoogleId(String googleId) {
        return userRepository.findByGoogleId(googleId)
                .orElseThrow(() -> new BadCredentialsException("Authenticated user not found"));
    }

    @Transactional(readOnly = true)
    public UserProfileDto getProfile(String googleId) {
        User user = findByGoogleId(googleId);
        List<Booking> bookings = bookingRepository.findByUserIdOrderByBookedAtDesc(user.getId());
        LocalDateTime now = LocalDateTime.now();

        long upcoming = bookings.stream()
                .filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED)
                .filter(booking -> booking.getSlot().getStartTime().isAfter(now))
                .count();
        long completed = bookings.stream()
                .filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED)
                .filter(booking -> !booking.getSlot().getStartTime().isAfter(now))
                .count();
        long cancelled = bookings.stream()
                .filter(booking -> booking.getStatus() == BookingStatus.CANCELLED)
                .count();

        return UserProfileDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .pictureUrl(user.getPictureUrl())
                .role(user.getRole())
                .totalBookings(bookings.size())
                .upcomingBookings(upcoming)
                .completedBookings(completed)
                .cancelledBookings(cancelled)
                .build();
    }

    @Transactional(readOnly = true)
    public List<BookingDto> getBookings(String googleId) {
        User user = findByGoogleId(googleId);
        return bookingRepository.findByUserIdOrderByBookedAtDesc(user.getId())
                .stream()
                .map(this::toBookingDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BookingDto> getUpcomingBookings(String googleId) {
        User user = findByGoogleId(googleId);
        return bookingRepository.findUpcomingByUserId(user.getId(), LocalDateTime.now())
                .stream()
                .map(this::toBookingDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BookingDto> getPastBookings(String googleId) {
        User user = findByGoogleId(googleId);
        return bookingRepository.findPastByUserId(user.getId(), LocalDateTime.now())
                .stream()
                .map(this::toBookingDto)
                .collect(Collectors.toList());
    }

    private User createUser(String googleId, String email, String name, String pictureUrl) {
        User user = User.builder()
                .googleId(googleId)
                .email(email)
                .name(name)
                .pictureUrl(pictureUrl)
                .role(Role.USER)
                .build();
        return userRepository.save(user);
    }

    private User attachGoogleLogin(User user, String googleId, String name, String pictureUrl) {
        user.setGoogleId(googleId);
        return updateProfile(user, name, pictureUrl);
    }

    private User updateProfile(User user, String name, String pictureUrl) {
        if (name != null && !name.isBlank()) {
            user.setName(name);
        }
        if (pictureUrl != null && !pictureUrl.isBlank()) {
            user.setPictureUrl(pictureUrl);
        }
        return userRepository.save(user);
    }

    private BookingDto toBookingDto(Booking booking) {
        return BookingDto.builder()
                .id(booking.getId())
                .slotId(booking.getSlot().getId())
                .expertId(booking.getSlot().getExpert().getId())
                .expertName(booking.getSlot().getExpert().getName())
                .startTime(booking.getSlot().getStartTime())
                .endTime(booking.getSlot().getEndTime())
                .amountPaid(booking.getAmountPaid())
                .currency(booking.getCurrency())
                .status(booking.getStatus().name())
                .bookedAt(booking.getBookedAt())
                .build();
    }
}
