package com.fightmind.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Typed, validated wrapper for all custom application properties.
 *
 * Values are read from application.yml → .env at startup.
 * Spring will fail-fast with a clear error message if any
 * required property is missing or invalid.
 */
@Data
@Validated
@Component
@ConfigurationProperties
public class AppProperties {

    /** JWT authentication settings */
    @Data
    public static class Jwt {
        @NotBlank(message = "jwt.secret must not be blank")
        @Size(min = 32, message = "jwt.secret must be at least 32 characters for HS256 security")
        private String secret;

        @Min(value = 3600000, message = "jwt.expiryMs must be at least 1 hour")
        private long expiryMs = 86_400_000L; // default: 24 hours
    }

    /** Python AI service connection settings */
    @Data
    public static class PythonService {
        @NotBlank(message = "python.service.url must not be blank")
        private String url;
        private int connectTimeoutMs = 5_000;
        private int readTimeoutMs   = 30_000;
    }

    private final Jwt           jwt           = new Jwt();
    private final PythonService pythonService = new PythonService();
}
