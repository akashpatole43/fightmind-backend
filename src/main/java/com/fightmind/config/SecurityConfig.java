package com.fightmind.config;

import com.fightmind.auth.oauth2.CustomOAuth2UserService;
import com.fightmind.auth.oauth2.OAuth2SuccessHandler;
import com.fightmind.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * SecurityConfig — central Spring Security configuration.
 *
 * Covers:
 *  1. JWT stateless session (no cookies / no HttpSession)
 *  2. CORS policy (React frontend on port 5173)
 *  3. Route-level access control:
 *       PUBLIC      → auth, oauth2, actuator health/info, swagger
 *       ROLE_USER   → chat, user profile, events
 *       ROLE_ADMIN  → /api/admin/**, /actuator/metrics, /actuator/prometheus
 *  4. Google OAuth2 login flow
 *  5. BCrypt password encoder (strength 12)
 *  6. @EnableMethodSecurity for @PreAuthorize annotations on service methods
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity           // enables @PreAuthorize, @Secured on methods
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter   jwtAuthFilter;
    private final UserDetailsService        userDetailsService;
    private final CustomOAuth2UserService   oAuth2UserService;
    private final OAuth2SuccessHandler      oAuth2SuccessHandler;

    // ── Password Encoder ────────────────────────────────────────────────────

    /**
     * BCryptPasswordEncoder with strength 12 (2^12 = 4096 hash rounds).
     * Strength 12 is the recommended production setting — strong against brute force
     * while hashing in ~300ms (acceptable UX).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    // ── Authentication Provider ─────────────────────────────────────────────

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    // ── CORS ────────────────────────────────────────────────────────────────

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of(
                "http://localhost:5173",     // React dev server
                "http://localhost:3000"      // alternative dev port
        ));
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("*"));
        cors.setAllowCredentials(true);
        cors.setMaxAge(3600L);              // preflight cache: 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }

    // ── Security Filter Chain ───────────────────────────────────────────────

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ── Disable CSRF (not needed — we use stateless JWT) ──────────
            .csrf(AbstractHttpConfigurer::disable)

            // ── CORS (React frontend) ──────────────────────────────────────
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // ── Stateless session (JWT only — no HttpSession) ──────────────
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ══════════════════════════════════════════════════════════════
            // ROUTE ACCESS CONTROL
            // Keep most-specific rules first; fall through to most-general.
            // ══════════════════════════════════════════════════════════════
            .authorizeHttpRequests(auth -> auth

                // ── PUBLIC — no token required ─────────────────────────
                .requestMatchers(
                        "/api/auth/register",
                        "/api/auth/login",
                        "/oauth2/**",
                        "/login/oauth2/**"
                ).permitAll()

                // Swagger UI (dev only — can be disabled in prod via profile)
                .requestMatchers(
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**"
                ).permitAll()

                // Actuator — health + info are public (Docker, UptimeRobot)
                .requestMatchers(
                        "/actuator/health",
                        "/actuator/info"
                ).permitAll()

                // ── ADMIN ONLY — metrics, prometheus, admin API ─────────
                .requestMatchers(
                        "/actuator/metrics/**",
                        "/actuator/prometheus"
                ).hasRole("ADMIN")

                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // ── Any other actuator endpoint is fully blocked ─────────
                .requestMatchers("/actuator/**").denyAll()

                // ── AUTHENTICATED (ROLE_USER or ROLE_ADMIN) ─────────────
                .requestMatchers(
                        "/api/chat/**",
                        "/api/user/**",
                        "/api/events/**"
                ).authenticated()

                // All other requests must be authenticated
                .anyRequest().authenticated()
            )

            // ── Google OAuth2 Login ────────────────────────────────────────
            .oauth2Login(oauth2 -> oauth2
                    .userInfoEndpoint(userInfo ->
                            userInfo.userService(oAuth2UserService))
                    .successHandler(oAuth2SuccessHandler)
            )

            // ── JWT Filter (runs before Spring's UsernamePasswordAuthFilter) ─
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
