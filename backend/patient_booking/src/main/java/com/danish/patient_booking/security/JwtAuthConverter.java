package com.danish.patient_booking.security;



package com.danish.patient_booking.security;

import com.danish.patient_booking.entity.User;
import com.danish.patient_booking.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserService userService;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {

        // 1. Extract claims from Google ID token
        String googleId = jwt.getSubject();          // Google's unique user ID
        String email    = jwt.getClaim("email");
        String name     = jwt.getClaim("name");
        String picture  = jwt.getClaim("picture");

        // 2. Find or create user in YOUR database
        //    First login → creates user row with role USER
        //    Subsequent logins → just fetches existing user
        User user = userService.findOrCreateUser(googleId, email, name, picture);

        // 3. Map your DB role → Spring Security authority
        //    MUST be prefixed with ROLE_ for hasRole() to work
        Collection<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );

        // 4. Return token with authorities — Spring Security uses this
        //    for all .hasRole() and .authenticated() checks downstream
        return new JwtAuthenticationToken(jwt, authorities, googleId);
    }
}