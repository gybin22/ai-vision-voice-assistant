package com.example.assistant.model;

public record VisionChatResult(
        String answer,
        String model,
        int inputTokens,
        int outputTokens,
        int totalTokens,
        double providerCostAmountYuan
) {}
