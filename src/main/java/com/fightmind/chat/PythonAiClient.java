package com.fightmind.chat;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Client that communicates strictly with the local Python FastAPI service.
 * 
 * Includes Spring Retry (@Retryable) configuration:
 * If the python service is restarting, disconnected, or drops the request,
 * this will automatically try again up to 3 times, waiting progressively
 * longer between each attempt (1s, 2s, 4s).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PythonAiClient {

    private final RestClient pythonAiRestClient;

    // ── DTOs matching the FastAPI schemas ──────────────────────────────────
    @Data
    @Builder
    public static class PythonRequest {
        private String query;
        private String sport;          // Optional sport context to guide Gemini
        private String skill_level;    // User's mapped skill level (e.g. beginner)
        private String image_url;      // Optional vision support
    }

    @Data
    public static class PythonResponse {
        private String answer;
        private String intent;
        private String sport;
        private Double confidence;
        private String error;
    }

    /**
     * Executes the POST request to the Python backend.
     * 
     * @Retryable applies to 5xx errors or underlying network dropouts (ResourceAccessException).
     * Does NOT retry on 4xx (Client/Bad Request) because sending the exact same 
     * bad payload twice will just fail twice.
     */
    @Retryable(
            retryFor = {ResourceAccessException.class, RestClientException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2) // 1s, 2s, 4s
    )
    public PythonResponse askPythonModel(PythonRequest request) {
        log.info("Sending request to Python AI Service. Query length: {}", 
                request.getQuery() != null ? request.getQuery().length() : 0);

        return pythonAiRestClient.post()
                .uri("/api/v1/ask")
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<PythonResponse>() {});
    }
}
