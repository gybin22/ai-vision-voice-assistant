package com.example.assistant.model;

public record CachedVisionAnswer(
        String answer,
        String model,
        int inputTokens,
        int outputTokens,
        double estimatedCost
) {}
