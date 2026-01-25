package com.danish.patient_booking.controller;

import com.danish.patient_booking.dto.UserDto;
import com.danish.patient_booking.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/me")
    public UserDto me(Authentication authentication) {
        return UserDto.from(userService.findByGoogleId(authentication.getName()));
    }
}
