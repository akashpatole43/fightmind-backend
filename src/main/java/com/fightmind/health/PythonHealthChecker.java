package com.fightmind.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Executes a completely independent health ping exactly once when Tomcat finishes booting.
 * Maintains the internal boolean flag tracking whether Python is alive.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PythonHealthChecker {

    private final RestClient pythonAiRestClient;
    
    // Thread-safe flag readable by the Chat Service and Actuator
    private final AtomicBoolean pythonReady = new AtomicBoolean(false);

    /**
     * Instantly triggered the moment the Spring Boot context fully starts.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("Java Backend is fully UP. Running initial ping to Python AI Service...");
        ping();
    }

    /**
     * Executes the HTTP GET /health request to FastAPI.
     * @return true if successful 200 OK.
     */
    public boolean ping() {
        try {
            String response = pythonAiRestClient.get()
                    .uri("/health")
                    .retrieve()
                    .body(String.class);

            if (response != null && response.contains("ok")) {
                if (!pythonReady.get()) {
                    log.info("✅ Python AI Service is online and ready!");
                    pythonReady.set(true);
                }
                return true;
            }
        } catch (Exception ex) {
            if (pythonReady.get() || pythonReady.get() == false) { // Log once or log transitions
                log.warn("⚠️ Python AI Service is completely unreachable. Message: {}. Background job will keep retrying...", ex.getMessage());
            }
            pythonReady.set(false);
        }
        return false;
    }

    public boolean isReady() {
        return pythonReady.get();
    }
}
