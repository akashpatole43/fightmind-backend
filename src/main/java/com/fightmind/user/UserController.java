package com.fightmind.user;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Controller for managing the logged-in user's profile.
 * Protected by Spring Security — requires valid JWT token.
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Automatically extracts the user ID from the JWT via @AuthenticationPrincipal.
     * The argument is of type Long because JwtAuthenticationFilter instantiated
     * UsernamePasswordAuthenticationToken with the Long ID as the principal.
     */
    @GetMapping("/profile")
    public ResponseEntity<UserProfileDto> getMyProfile(@AuthenticationPrincipal Long userId) {

        User user = userService.getUserById(userId);

        UserProfileDto response = UserProfileDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole())
                .provider(user.getProvider())
                .skillLevel(user.getSkillLevel())
                .joinedAt(user.getCreatedAt())
                .build();

        return ResponseEntity.ok(response);
    }

    // ── DTO ──────────────────────────────────────────────
    @Data
    @Builder
    public static class UserProfileDto {
        private Long id;
        private String email;
        private String username;
        private String role;
        private String provider;
        private String skillLevel;
        private Instant joinedAt;
    }
}
