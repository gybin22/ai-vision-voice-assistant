package com.example.assistant.security;

import com.example.assistant.config.AssistantProperties;
import com.example.assistant.entity.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    public enum TokenType {
        ACCESS,
        REFRESH
    }

    private final AssistantProperties properties;
    private final SecretKey key;

    public JwtService(AssistantProperties properties) {
        this.properties = properties;
        String secret = properties.getAuth().getJwtSecret();
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("assistant.auth.jwt-secret 至少需要 32 字节。请使用环境变量 ASSISTANT_AUTH_JWT_SECRET 配置生产密钥。");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UserEntity user) {
        return generate(user, TokenType.ACCESS, properties.getAuth().getAccessTokenMinutes() * 60L);
    }

    public String generateRefreshToken(UserEntity user) {
        return generate(user, TokenType.REFRESH, properties.getAuth().getRefreshTokenDays() * 24L * 60L * 60L);
    }

    public long accessTokenExpiresInSeconds() {
        return properties.getAuth().getAccessTokenMinutes() * 60L;
    }

    public long refreshTokenExpiresInSeconds() {
        return properties.getAuth().getRefreshTokenDays() * 24L * 60L * 60L;
    }

    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(properties.getAuth().getJwtIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Token 无效或已过期。", e);
        }
    }

    public boolean isTokenType(Claims claims, TokenType type) {
        return type.name().equalsIgnoreCase(String.valueOf(claims.get("typ")));
    }

    public Long userId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

    public int tokenVersion(Claims claims) {
        Object value = claims.get("ver");
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private String generate(UserEntity user, TokenType type, long expiresInSeconds) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expiresInSeconds);

        return Jwts.builder()
                .issuer(properties.getAuth().getJwtIssuer())
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("typ", type.name())
                .claim("ver", user.getTokenVersion())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
    }
}
