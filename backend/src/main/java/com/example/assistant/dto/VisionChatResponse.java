package com.example.assistant.dto;

import com.example.assistant.dto.billing.BillingUsageDTO;

public record VisionChatResponse(
        String requestId,
        String sessionId,
        String answer,
        String model,
        boolean cached,
        ResponseUsageDTO usage,
        BillingUsageDTO billing,
        long latencyMs
) {}
