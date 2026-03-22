package com.fightmind.admin;

import com.fightmind.admin.dto.AdminStatsDto;
import com.fightmind.admin.dto.UserSummaryDto;
import com.fightmind.chat.ChatMessageRepository;
import com.fightmind.chat.dto.ChatResponse;
import com.fightmind.exception.ResourceNotFoundException;
import com.fightmind.user.User;
import com.fightmind.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ChatMessageRepository chatRepository;

    @Transactional(readOnly = true)
    public AdminStatsDto getSystemStats() {
        return AdminStatsDto.builder()
                .totalUsers(userRepository.count())
                .totalMessages(chatRepository.count())
                .cachedResponses(chatRepository.countByCachedTrue())
                .messagesWithImages(chatRepository.countByImageUrlIsNotNull())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<UserSummaryDto> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(user -> UserSummaryDto.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .provider(user.getProvider())
                        .createdAt(user.getCreatedAt())
                        .build()
                );
    }

    @Transactional(readOnly = true)
    public Page<ChatResponse> getUserHistory(Long targetUserId, Pageable pageable) {
        // Confirm user exists first
        if (!userRepository.existsById(targetUserId)) {
            throw new ResourceNotFoundException("User " + targetUserId + " not found");
        }

        return chatRepository.findByUserIdOrderByCreatedAtDesc(targetUserId, pageable)
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

    @Transactional
    public void updateUserRole(Long targetUserId, String newRole) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User " + targetUserId + " not found"));
        
        user.setRole(newRole);
        userRepository.save(user);
        log.warn("Admin changed role of user {} to {}", user.getEmail(), newRole);
    }

    @Transactional
    public void deleteUser(Long targetUserId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User " + targetUserId + " not found"));
        
        // This will cascade delete chat_messages because of the Flyway schema ON DELETE CASCADE
        userRepository.delete(user);
        log.warn("Admin permanently deleted user {}", user.getEmail());
    }
}
