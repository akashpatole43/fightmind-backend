package com.fightmind.chat;

import com.fightmind.chat.dto.ChatRequest;
import com.fightmind.chat.dto.ChatResponse;
import com.fightmind.user.User;
import com.fightmind.user.UserRepository;
import com.fightmind.health.PythonHealthChecker;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.CompletableFuture;

/**
 * The core orchestrator for the Chat Module.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatRepository;
    private final UserRepository userRepository;
    private final PythonAiClient pythonAiClient;
    private final ChatCacheService cacheService;
    private final PythonHealthChecker healthChecker;
    private final Tracer tracer;

    /**
     * Executes entirely on the "aiTaskExecutor" thread pool (not the Tomcat request thread).
     * 1. Checks Redis cache
     * 2. If miss, calls Python AI (waiting up to 30s)
     * 3. Saves to PostgreSQL DB
     * 4. Saves to Redis cache
     * 5. Returns DTO
     */
    @Async("aiTaskExecutor")
    @Transactional
    public CompletableFuture<ChatResponse> askQuestion(Long userId, ChatRequest request) {
        log.info("Processing chat request for User {} on thread {}", userId, Thread.currentThread().getName());

        if (!healthChecker.isReady()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, 
                    "The FightMind AI Engine is currently booting or undergoing maintenance. Please try again in a few moments.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String userQuery = request.getQuery();
        boolean isCached = false;

        // 1. Check Cache
        PythonAiClient.PythonResponse aiResponse = cacheService.getCachedResponse(userQuery);

        // 2. Cache Miss -> Call Python Model
        if (aiResponse == null) {
            log.info("Cache miss, delegating to Python AI Service...");
            PythonAiClient.PythonRequest pyRequest = PythonAiClient.PythonRequest.builder()
                    .query(userQuery)
                    .skill_level(user.getSkillLevel())
                    .image_url(request.getImageUrl())
                    .build();

            try {
                aiResponse = pythonAiClient.askPythonModel(pyRequest);

                // Cache the successful response for future identical queries
                cacheService.cacheResponse(userQuery, aiResponse);

            } catch (Exception ex) {
                log.error("Failed to get response from Python AI", ex);
                // Fallback response so the user isn't completely stranded
                aiResponse = new PythonAiClient.PythonResponse();
                aiResponse.setAnswer("I'm sorry, my training circuits are currently offline. Please try again later.");
                aiResponse.setError(ex.getMessage());
            }
        } else {
            isCached = true;
        }

        // 3. Persist to PostgreSQL Database for history tracking
        String traceId = getTraceId();
        
        ChatMessage message = ChatMessage.builder()
                .user(user)
                .query(userQuery)
                .answer(aiResponse.getAnswer())
                .intent(aiResponse.getIntent())
                .sport(aiResponse.getSport())
                .confidence(aiResponse.getConfidence())
                .cached(isCached)
                .imageUrl(request.getImageUrl())
                .traceId(traceId)
                .build();

        message = chatRepository.save(message);

        // 4. Return formatted response to the web controller
        ChatResponse respDto = ChatResponse.builder()
                .id(message.getId())
                .query(message.getQuery())
                .answer(message.getAnswer())
                .cached(message.isCached())
                .intent(message.getIntent())
                .sport(message.getSport())
                .imageUrl(message.getImageUrl())
                .timestamp(message.getCreatedAt())
                .traceId(traceId)
                .build();

        return CompletableFuture.completedFuture(respDto);
    }

    /**
     * Retrieves paginated chat history for the user profile page.
     */
    @Transactional(readOnly = true)
    public Page<ChatResponse> getUserHistory(Long userId, Pageable pageable) {
        return chatRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(msg -> ChatResponse.builder()
                        .id(msg.getId())
                        .query(msg.getQuery())
                        .answer(msg.getAnswer())
                        .cached(msg.isCached())
                        .intent(msg.getIntent())
                        .sport(msg.getSport())
                        .imageUrl(msg.getImageUrl())
                        .timestamp(msg.getCreatedAt())
                        .traceId(msg.getTraceId())
                        .build()
                );
    }

    private String getTraceId() {
        if (tracer != null && tracer.currentSpan() != null) {
            return tracer.currentSpan().context().traceId();
        }
        return "none";
    }
}
