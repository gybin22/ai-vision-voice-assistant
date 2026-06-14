package com.example.assistant.dto.history;

import java.util.List;

public record ChatHistoryDayDTO(
        String date,
        String label,
        List<ChatSessionSummaryDTO> sessions
) {}
