package com.example.assistant.dto.auth;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 80, message = "昵称最多 80 个字符")
        String nickname,

        @Size(max = 512, message = "头像地址最多 512 个字符")
        String avatarUrl
) {}
