package com.example.assistant.model;

import java.util.List;

public record VisionChatCommand(
        String requestId,
        String sessionId,
        String question,
        List<VisionFrame> frames,
        String frameMetadataJson,
        boolean enableHistory,
        int maxOutputTokens,
        List<ChatMessage> history,
        String systemPrompt
) {}
