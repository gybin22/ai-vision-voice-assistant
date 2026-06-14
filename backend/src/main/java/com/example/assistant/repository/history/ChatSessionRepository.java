package com.example.assistant.repository.history;

import com.example.assistant.entity.history.ChatSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSessionEntity, Long> {
    Optional<ChatSessionEntity> findByUserIdAndSessionId(Long userId, String sessionId);

    List<ChatSessionEntity> findByUserIdOrderByLastMessageAtDesc(Long userId);

    long countByUserId(Long userId);

    long deleteByUserId(Long userId);

    long deleteByUserIdAndSessionId(Long userId, String sessionId);
}
