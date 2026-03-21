package com.fightmind.jwt;

import com.fightmind.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Core JWT utility class.
 *
 * Responsibilities:
 * 1. Generating tokens (for local login and Google OAuth2 login)
 * 2. Extracting claims (userId and role) from tokens
 * 3. Validating tokens (checking signature, expiration, malformed structure)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final AppProperties appProperties;

    // The key is cached so we don't recreate it on every request
    private SecretKey getSigningKey() {
        byte[] keyBytes = appProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates a token containing the user's ID as the subject, and their role as a claim.
     * Making the token contain the role makes auth stateless (we don't need a DB query
     * on every single HTTP request to check if they are an ADMIN).
     */
    public String generateToken(Long userId, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + appProperties.getJwt().getExpiryMs());

        return Jwts.builder()
                .subject(Long.toString(userId))     // The principal (user ID)
                .claim("role", role)                // Custom claim for authorization
                .issuedAt(new Date())               // iat
                .expiration(expiryDate)             // exp
                .signWith(getSigningKey())          // HS256 signature
                .compact();
    }

    /**
     * Extracts the User ID from the token subject.
     */
    public Long getUserIdFromJWT(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return Long.parseLong(claims.getSubject());
    }

    /**
     * Extracts the role claim (e.g. "ROLE_USER" or "ROLE_ADMIN") from the token.
     */
    public String getRoleFromJWT(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("role", String.class);
    }

    /**
     * Validates the token signature, expiration, and format.
     * Logs the specific reason if a token is rejected.
     */
    public boolean validateToken(String authToken) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(authToken);
            return true;
        } catch (SecurityException | MalformedJwtException ex) {
            log.warn("Invalid JWT signature/format");
        } catch (ExpiredJwtException ex) {
            log.warn("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            log.warn("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            log.warn("JWT claims string is empty");
        }
        return false;
    }
}
