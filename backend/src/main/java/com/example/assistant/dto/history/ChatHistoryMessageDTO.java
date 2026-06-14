package com.example.assistant.dto.history;

import java.time.Instant;

public record ChatHistoryMessageDTO(
        Long id,
        String role,
        String content,
        String requestId,
        String modelName,
        int inputTokens,
        int outputTokens,
        int totalTokens,
        long chargedTokens,
        Instant createdAt
) {}
