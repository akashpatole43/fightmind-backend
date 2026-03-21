package com.fightmind.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Filter that intercepts ALL incoming HTTP requests.
 * 
 * 1. Checks for the "Authorization: Bearer <token>" header
 * 2. Validates the token (signature, expiration)
 * 3. If valid, extracts the User ID and Role, and establishes authentication
 *    in the Spring SecurityContext.
 * 
 * Note: Because the JWT contains the `role`, we DO NOT need to query the database
 * on every request. This makes the architecture completely stateless and fast.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                
                Long userId = tokenProvider.getUserIdFromJWT(jwt);
                String role = tokenProvider.getRoleFromJWT(jwt);

                // Create the authentication token using userId as the Principal
                // We don't load the full User object from DB to save a database hit
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userId, 
                        null, 
                        Collections.singletonList(new SimpleGrantedAuthority(role))
                );
                
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Save auth to the SecurityContext so @RestController and @PreAuthorize know who the user is
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the JWT string from the HTTP Authorization header.
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
