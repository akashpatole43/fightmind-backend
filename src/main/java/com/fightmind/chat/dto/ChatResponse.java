package com.fightmind.chat.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Standard format returned to the React frontend.
 * Matches the required props for a standard chat-bubble UI component.
 */
@Data
@Builder
public class ChatResponse {
    private Long id;
    private String query;
    private String answer;
    
    // UI can optionally show badges like ⚡ Cached, 🥊 Boxing, ℹ️ Rules
    private boolean cached;
    private String intent;
    private String sport;
    private String imageUrl;
    
    private Instant timestamp;
    
    // TraceId specifically returned so a user can click "Report Bad Answer" 
    // and send us the Trace ID for exact debugging.
    private String traceId;
}
