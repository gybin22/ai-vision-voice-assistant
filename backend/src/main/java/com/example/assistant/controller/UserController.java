package com.example.assistant.controller;

import com.example.assistant.dto.auth.AuthUserDTO;
import com.example.assistant.dto.auth.UpdateProfileRequest;
import com.example.assistant.security.UserPrincipal;
import com.example.assistant.service.auth.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    public AuthUserDTO me(@AuthenticationPrincipal UserPrincipal principal) {
        return authService.profile(principal);
    }

    @PutMapping("/me")
    public AuthUserDTO updateMe(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return authService.updateProfile(principal, request);
    }
}
