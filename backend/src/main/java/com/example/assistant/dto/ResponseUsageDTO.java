package com.example.assistant.dto;

public record ResponseUsageDTO(
        int inputTokens,
        int outputTokens,
        long imageBytes,
        double estimatedCost
) {}
