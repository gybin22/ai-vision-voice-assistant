package com.example.assistant.dto;

public record ResponseUsageDTO(
        int inputTokens,
        int outputTokens,
        int totalTokens,
        long imageBytes,
        double providerCostAmountYuan
) {}
