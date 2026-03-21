package com.fightmind.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * WebClientConfig — creates a typed RestClient for calling the Python AI service.
 *
 * Uses Spring 6's RestClient (modern replacement for RestTemplate):
 *  - 5s connection timeout  → fail fast if Python is down
 *  - 30s read timeout       → allow Gemini enough time to respond
 *  - Base URL from AppProperties (loaded from .env)
 *
 * PythonAiClient injects this bean and wraps it with @Retryable logic.
 */
@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    private final AppProperties appProperties;

    @Bean
    public RestClient pythonAiRestClient() {
        AppProperties.PythonService cfg = appProperties.getPythonService();

        org.springframework.http.client.SimpleClientHttpRequestFactory factory = 
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
                
        // Fallback: timeouts provided in Application properties 
        factory.setConnectTimeout(cfg.getConnectTimeoutMs());
        factory.setReadTimeout(cfg.getReadTimeoutMs());

        return RestClient.builder()
                .baseUrl(cfg.getUrl())
                .requestFactory(factory)
                .build();
    }
}
