package com.example.assistant.dto.history;

import java.time.Instant;

public record ChatSessionSummaryDTO(
        String sessionId,
        String title,
        int messageCount,
        String lastMessagePreview,
        Instant createdAt,
        Instant updatedAt,
        Instant lastMessageAt
) {}
