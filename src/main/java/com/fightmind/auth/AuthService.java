package com.fightmind.auth;

import com.fightmind.auth.dto.AuthResponse;
import com.fightmind.auth.dto.LoginRequest;
import com.fightmind.auth.dto.RegisterRequest;
import com.fightmind.exception.BadRequestException;
import com.fightmind.jwt.JwtTokenProvider;
import com.fightmind.user.User;
import com.fightmind.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already in use.");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username is already taken.");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .provider("LOCAL")
                // Roles default to ROLE_USER and skill defaults to UNKNOWN in the entity
                .build();

        user = userRepository.save(user);
        log.info("New user registered via LOCAL auth: {}", user.getEmail());

        String jwt = tokenProvider.generateToken(user.getId(), user.getRole());
        return mapToResponse(user, jwt);
    }

    public AuthResponse login(LoginRequest request) {
        // Authenticate credentials against CustomUserDetailsService
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        log.info("User logged in via LOCAL auth: {}", user.getEmail());

        String jwt = tokenProvider.generateToken(user.getId(), user.getRole());
        return mapToResponse(user, jwt);
    }

    private AuthResponse mapToResponse(User user, String token) {
        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
