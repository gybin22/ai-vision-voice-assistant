package com.example.assistant.dto;

public record ErrorResponse(
        String requestId,
        String code,
        String message
) {}
