package com.example.assistant.model;

public record VisionFrame(
        int sequence,
        String filename,
        byte[] bytes,
        String mimeType,
        String role,
        Long capturedAt,
        Long offsetMs,
        Integer width,
        Integer height,
        Long size
) {}
