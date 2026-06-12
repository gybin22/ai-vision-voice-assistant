package com.example.assistant.model;

import java.util.List;

public record VisionChatCommand(
        String requestId,
        String sessionId,
        String question,
        byte[] imageBytes,
        String imageMimeType,
        boolean enableHistory,
        int maxOutputTokens,
        List<ChatMessage> history,
        String systemPrompt
) {}
