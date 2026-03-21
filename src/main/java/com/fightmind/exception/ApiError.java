package com.fightmind.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

/**
 * Standard JSON response body for all API errors.
 *
 * Example:
 * {
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "User with ID 5 not found",
 *   "timestamp": "2026-03-22T10:00:00Z",
 *   "traceId": "4bf92f3577b34da6"
 * }
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {
    private final int status;
    private final String error;
    private final String message;
    private final Instant timestamp;
    private final String traceId;

    // Optional: for field validation errors (e.g. {"email": "must not be blank"})
    private final Map<String, String> validationErrors;
}
