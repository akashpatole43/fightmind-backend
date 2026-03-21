package com.fightmind.exception;

import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global API Error Handler — catches all exceptions thrown in the application
 * and maps them to a consistent JSON ApiError response.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final Tracer tracer;

    // ── Custom Application Exceptions (our 400, 401, 404s) ──────────────────
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException ex) {
        log.warn("API Exception: {} - {}", ex.getStatus(), ex.getMessage());
        return buildErrorResponse(ex.getStatus(), ex.getMessage(), null);
    }

    // ── Validation Errors (@Valid on RequestBody maps/DTOs) ─────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation Error: {}", errors);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation failed", errors);
    }

    // ── Invalid Spring MVC Paths (404) ──────────────────────────────────────
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResourceFound(NoResourceFoundException ex) {
        log.warn("Route not found: {}", ex.getResourcePath());
        return buildErrorResponse(HttpStatus.NOT_FOUND, "API route not found", null);
    }

    // ── Spring Security: Access Denied (403) ────────────────────────────────
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access Denied (403): {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Access denied to this resource", null);
    }

    // ── Spring Security: Authentication Exception (401) ─────────────────────
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication failed (401): {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid authentication token", null);
    }

    // ── Uncaught Exceptions (500) ───────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAllOtherExceptions(Exception ex) {
        // Log the full stack trace for 500s (since it's an unexpected bug)
        log.error("Unhandled Exception (500): ", ex);

        // Never expose the exception message to the client for 500 errors
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected server error occurred",
                null
        );
    }

    // ── Helper ──────────────────────────────────────────────────────────────
    private ResponseEntity<ApiError> buildErrorResponse(HttpStatus status, String message, Map<String, String> validationErrors) {
        ApiError apiError = ApiError.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .timestamp(Instant.now())
                .traceId(getTraceId())
                .validationErrors(validationErrors)
                .build();

        return new ResponseEntity<>(apiError, status);
    }

    private String getTraceId() {
        if (tracer != null && tracer.currentSpan() != null) {
            return tracer.currentSpan().context().traceId();
        }
        return "none";
    }
}
