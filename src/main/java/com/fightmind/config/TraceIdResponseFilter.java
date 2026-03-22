package com.fightmind.config;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that executes on *every single request* to the backend.
 * Micrometer automatically assigns a Trace ID to the request thread.
 * This filter extracts that ID and injects it into the HTTP Response Headers,
 * so the React frontend can easily grab it and display it in error popups!
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class TraceIdResponseFilter extends OncePerRequestFilter {

    private final Tracer tracer;

    private static final String TRACE_ID_HEADER_NAME = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        // 1. Check if Micrometer has assigned an active span to this thread
        Span currentSpan = tracer.currentSpan();
        
        if (currentSpan != null) {
            String traceId = currentSpan.context().traceId();
            // 2. Inject it into the response header immediately
            response.setHeader(TRACE_ID_HEADER_NAME, traceId);
        }

        // 3. Continue the request normally
        filterChain.doFilter(request, response);
    }
}
