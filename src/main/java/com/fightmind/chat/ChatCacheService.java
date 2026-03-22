package com.fightmind.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Service to interact specifically with Redis for caching AI responses.
 *
 * We don't use @Cacheable here because the caching logic is complex:
 * We want to look up by the EXACT user query (ignoring case/whitespace)
 * and only cache if the AI was highly confident.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    // Cache responses for 1 hour to handle rapid re-asking, but eventually 
    // fall off so the bot can learn new information dynamically.
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    /**
     * Checks if we have a recent, high-confidence answer for this exact query.
     */
    public PythonAiClient.PythonResponse getCachedResponse(String userQuery) {
        String key = generateCacheKey(userQuery);
        PythonAiClient.PythonResponse response = 
                (PythonAiClient.PythonResponse) redisTemplate.opsForValue().get(key);

        if (response != null) {
            log.info("Redis Cache HIT for query: '{}'", userQuery);
            return response;
        }

        return null;
    }

    /**
     * Saves the AI response to Redis. Only saves if the Python service
     * was relatively confident (so we don't permanently cache "I don't know").
     */
    public void cacheResponse(String userQuery, PythonAiClient.PythonResponse response) {
        if (response == null || response.getError() != null) {
            return; // Don't cache errors
        }

        // Only cache if the Python AI was >70% confident in the martial arts intent
        if (response.getConfidence() != null && response.getConfidence() > 0.7) {
            String key = generateCacheKey(userQuery);
            redisTemplate.opsForValue().set(key, response, CACHE_TTL);
            log.debug("Redis Cache SET for query: '{}'", userQuery);
        }
    }

    /**
     * Normalizes the query so " What is a jab? " and "what is a jab?" hit the same cache entry.
     */
    private String generateCacheKey(String userQuery) {
        String normalizedQuery = userQuery.trim().toLowerCase().replaceAll("\\s+", " ");
        return "fightmind:chat:" + normalizedQuery;
    }
}
