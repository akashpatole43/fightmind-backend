package com.fightmind.auth.oauth2;

import com.fightmind.auth.CustomUserDetails;
import com.fightmind.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Triggered automatically by Spring Security IMMEDIATELY after
 * CustomOAuth2UserService finishes registering/loading the user.
 *
 * Job:
 * 1. Creates a JWT token for the user.
 * 2. Issues an HTTP 302 Redirect to the frontend (React) URL, embedding
 *    the fresh token in the query string so the frontend can save it to localStorage.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;

    // The frontend dev server URL (Vite default is 5173).
    // In production, this would be read from application.yml / env vars.
    @Value("${app.oauth2.redirect-uri:http://localhost:5173/oauth2/redirect}")
    private String frontendRedirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        // 1. Generate our own JWT token (we don't use the raw Google token for our APIs)
        String token = tokenProvider.generateToken(userDetails.getId(), userDetails.getUser().getRole());

        // 2. Redirect to frontend with the token
        // e.g., http://localhost:5173/oauth2/redirect?token=eyJhbGci...
        String targetUrl = UriComponentsBuilder.fromUriString(frontendRedirectUri)
                .queryParam("token", token)
                .build().toUriString();

        log.info("OAuth2 login successful for {}. Redirecting to frontend.", userDetails.getUsername());

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
