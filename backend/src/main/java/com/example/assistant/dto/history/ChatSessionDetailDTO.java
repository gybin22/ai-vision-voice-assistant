package com.example.assistant.dto.history;

import java.util.List;

public record ChatSessionDetailDTO(
        ChatSessionSummaryDTO session,
        List<ChatHistoryMessageDTO> messages
) {}
