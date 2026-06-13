package com.example.assistant.dto.auth;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        long refreshExpiresInSeconds,
        AuthUserDTO user
) {}
