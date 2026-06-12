package com.example.assistant.dto;

public record SessionUsageDTO(
        String sessionId,
        int requestCount,
        int requestLimit,
        double estimatedCost,
        int remainingRequests
) {}
