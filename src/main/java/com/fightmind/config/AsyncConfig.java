package com.fightmind.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * AsyncConfig — configures the dedicated thread pool for async AI calls.
 *
 * Why a dedicated executor?
 *  Calling the Python AI service takes 2-5 seconds (Gemini response time).
 *  Without a dedicated pool, all 200 Tomcat threads could be blocked waiting
 *  for AI responses, making the app unresponsive to new requests.
 *
 * With this executor:
 *  - Tomcat thread receives the request and immediately delegates to "ai-" pool
 *  - Tomcat thread is freed instantly to accept new connections
 *  - Up to 50 AI calls can run simultaneously; 100 more wait in the queue
 *  - If queue is full, RejectedExecutionException triggers the fallback response
 */
@EnableAsync
@Configuration
public class AsyncConfig {

    /**
     * Executor named "aiTaskExecutor" — referenced in @Async("aiTaskExecutor")
     * on ChatService methods that call the Python AI.
     */
    @Bean(name = "aiTaskExecutor")
    public Executor aiTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Always-on threads — immediately available for AI calls
        executor.setCorePoolSize(10);

        // Maximum threads during traffic burst
        executor.setMaxPoolSize(50);

        // How many tasks can queue up before rejection
        executor.setQueueCapacity(100);

        // Prefix makes AI threads instantly identifiable in logs/thread dumps
        executor.setThreadNamePrefix("ai-");

        // Wait up to 30s for running tasks to complete before shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }
}
