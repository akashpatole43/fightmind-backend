package com.fightmind;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * FightMind AI — Java Spring Boot Backend
 *
 * Entry point for the REST API that:
 *  - Authenticates users (email/password + Google OAuth2)
 *  - Orchestrates calls to the Python AI service
 *  - Persists chat history and user profiles to PostgreSQL
 *  - Provides an admin API for user management and monitoring
 *
 * Enabled capabilities:
 *  @EnableAsync       — allows @Async on ChatService.callPythonAi() so Tomcat threads
 *                       are never blocked waiting for the AI response
 *  @EnableScheduling  — powers PythonHealthScheduler (@Scheduled every 30s)
 *  @EnableRetry       — powers PythonAiClient (@Retryable with exponential backoff)
 *  @EnableCaching     — powers ChatCacheService (Redis caching of AI responses)
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableRetry
@EnableCaching
public class FightMindApplication {

    public static void main(String[] args) {
        SpringApplication.run(FightMindApplication.class, args);
    }
}
