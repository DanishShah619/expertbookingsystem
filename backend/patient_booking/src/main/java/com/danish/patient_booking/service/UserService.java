package com.danish.patient_booking.service;

import com.danish.patient_booking.enums.Role;
import com.danish.patient_booking.model.User;
import com.danish.patient_booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

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
}
