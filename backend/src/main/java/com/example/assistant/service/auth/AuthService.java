package com.example.assistant.service.auth;

import com.example.assistant.dto.auth.*;
import com.example.assistant.entity.UserEntity;
import com.example.assistant.entity.UserStatus;
import com.example.assistant.repository.UserRepository;
import com.example.assistant.security.JwtService;
import com.example.assistant.security.UserPrincipal;
import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

import static org.springframework.http.HttpStatus.*;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(CONFLICT, "该邮箱已经注册。");
        }

        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(normalizeNickname(request.nickname(), email));
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);

        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("邮箱或密码不正确。"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(FORBIDDEN, "账号已被禁用。");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("邮箱或密码不正确。");
        }

        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshTokenRequest request) {
        Claims claims = jwtService.parse(request.refreshToken());
        if (!jwtService.isTokenType(claims, JwtService.TokenType.REFRESH)) {
            throw new ResponseStatusException(UNAUTHORIZED, "refreshToken 类型不正确。", null);
        }

        Long userId = jwtService.userId(claims);
        int tokenVersion = jwtService.tokenVersion(claims);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "用户不存在。"));

        if (user.getStatus() != UserStatus.ACTIVE || user.getTokenVersion() != tokenVersion) {
            throw new ResponseStatusException(UNAUTHORIZED, "登录状态已失效，请重新登录。", null);
        }

        return buildAuthResponse(user);
    }

    @Transactional
    public void logout(UserPrincipal principal) {
        UserEntity user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "用户不存在。"));
        user.incrementTokenVersion();
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public AuthUserDTO profile(UserPrincipal principal) {
        UserEntity user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "用户不存在。"));
        return AuthUserDTO.from(user);
    }

    @Transactional
    public AuthUserDTO updateProfile(UserPrincipal principal, UpdateProfileRequest request) {
        UserEntity user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "用户不存在。"));

        if (request.nickname() != null) {
            String nickname = request.nickname().trim();
            if (!nickname.isBlank()) {
                user.setNickname(nickname);
            }
        }

        if (request.avatarUrl() != null) {
            String avatarUrl = request.avatarUrl().trim();
            user.setAvatarUrl(avatarUrl.isBlank() ? null : avatarUrl);
        }

        return AuthUserDTO.from(userRepository.save(user));
    }

    private AuthResponse buildAuthResponse(UserEntity user) {
        return new AuthResponse(
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user),
                jwtService.accessTokenExpiresInSeconds(),
                jwtService.refreshTokenExpiresInSeconds(),
                AuthUserDTO.from(user)
        );
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeNickname(String nickname, String email) {
        if (nickname != null && !nickname.trim().isBlank()) {
            return nickname.trim();
        }
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }
}
