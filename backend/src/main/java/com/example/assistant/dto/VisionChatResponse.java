package com.example.assistant.dto;

public record VisionChatResponse(
        String requestId,
        String sessionId,
        String answer,
        String model,
        boolean cached,
        ResponseUsageDTO usage,
        long latencyMs
) {}
