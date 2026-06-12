package com.example.assistant.model;

public record VisionFrame(
        int sequence,
        String filename,
        byte[] bytes,
        String mimeType
) {}
