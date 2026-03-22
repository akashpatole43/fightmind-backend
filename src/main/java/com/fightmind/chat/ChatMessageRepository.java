package com.fightmind.chat;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // Used by ChatController to render the user's scrolling message history
    Page<ChatMessage> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // Used by the Admin Controller for overall system statistics
    long countByCachedTrue();

    // Used by Admin Controller to count messages containing images
    long countByImageUrlIsNotNull();
}
