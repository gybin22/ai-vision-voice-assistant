package com.example.assistant.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Email(message = "邮箱格式不正确")
        @NotBlank(message = "邮箱不能为空")
        String email,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 72, message = "密码长度必须在 8 到 72 位之间")
        String password,

        @Size(max = 80, message = "昵称最多 80 个字符")
        String nickname
) {}
