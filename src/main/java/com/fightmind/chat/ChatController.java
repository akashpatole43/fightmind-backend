package com.fightmind.chat;

import com.fightmind.chat.dto.ChatRequest;
import com.fightmind.chat.dto.ChatResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

/**
 * REST API for the chat interface.
 * Requires Authentication (Authorization: Bearer <JWT>).
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * Async endpoint — Tomcat thread instantly returns a CompletableFuture.
     * The actual work (Redis -> Python -> Postgres) happens on "aiTaskExecutor".
     */
    @PostMapping("/send")
    public CompletableFuture<ResponseEntity<ChatResponse>> askQuestion(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ChatRequest request) {

        log.debug("Received chat request from user ID {}", userId);

        return chatService.askQuestion(userId, request)
                .thenApply(ResponseEntity::ok);
    }

    /**
     * Retrieves the paginated history of messages for the logged-in user.
     * Ordered by newest first.
     */
    @GetMapping("/history")
    public ResponseEntity<Page<ChatResponse>> getChatHistory(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("Fetching chat history page {} for user ID {}", page, userId);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ChatResponse> historyPage = chatService.getUserHistory(userId, pageable);
        
        return ResponseEntity.ok(historyPage);
    }
}
