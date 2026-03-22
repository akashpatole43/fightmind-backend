package com.fightmind.health;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Injects our internal Python health state directly into Spring Boot Actuator.
 * Now `GET /actuator/health` will return:
 * {
 *   "status": "UP",
 *   "components": {
 *     "pythonAiService": {"status": "UP"},
 *     "db": {"status": "UP"}
 *   }
 * }
 */
@Component("pythonAiService")
@RequiredArgsConstructor
public class PythonHealthIndicator implements HealthIndicator {

    private final PythonHealthChecker pythonHealthChecker;

    @Override
    public Health health() {
        if (pythonHealthChecker.isReady()) {
            return Health.up().withDetail("service", "FastAPI Python Backend").build();
        } else {
            return Health.down().withDetail("service", "FastAPI Python Backend (Unreachable)").build();
        }
    }
}
