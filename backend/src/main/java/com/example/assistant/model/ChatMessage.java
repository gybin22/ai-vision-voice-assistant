package com.example.assistant.model;

public record ChatMessage(
        String role,
        String content
) {}
