package com.fightmind.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Background worker that only acts if Python is currently offline.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PythonHealthScheduler {

    private final PythonHealthChecker pythonHealthChecker;

    /**
     * Runs every 30 seconds (30,000 ms).
     * If the Python API was down during startup, this will eventually detect
     * when it comes back up and tell the whole system to enable Chat.
     */
    @Scheduled(fixedDelay = 30000)
    public void periodicallyCheckPythonHealth() {
        // If it's already connected, we do not need to spam ping it.
        // (Actuator will handle manual checks if needed).
        if (!pythonHealthChecker.isReady()) {
            log.debug("Executing scheduled ping to offline Python service...");
            pythonHealthChecker.ping();
        }
    }
}
