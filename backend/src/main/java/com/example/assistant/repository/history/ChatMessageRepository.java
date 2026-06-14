package com.example.assistant.repository.history;

import com.example.assistant.entity.history.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {
    List<ChatMessageEntity> findByUserIdAndSessionIdOrderByCreatedAtAsc(Long userId, String sessionId);

    long countByUserId(Long userId);

    long deleteByUserId(Long userId);

    long deleteByUserIdAndSessionId(Long userId, String sessionId);
}
