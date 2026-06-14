package com.example.assistant.dto.auth;

import com.example.assistant.entity.UserEntity;
import com.example.assistant.entity.UserStatus;

import java.time.Instant;

public record AuthUserDTO(
        Long id,
        String email,
        String nickname,
        String avatarUrl,
        UserStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static AuthUserDTO from(UserEntity user) {
        return new AuthUserDTO(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
