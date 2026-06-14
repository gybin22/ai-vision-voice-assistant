package com.example.assistant.dto.history;

public record ClearChatHistoryResponse(
        long deletedSessions,
        long deletedMessages
) {}
