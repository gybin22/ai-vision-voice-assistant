package com.example.assistant.util;

import java.security.MessageDigest;
import java.util.HexFormat;

public final class ImageHashUtil {
    private ImageHashUtil() {}

    public static String cacheKey(String sessionId, String question, byte[] imageBytes) {
        String normalizedQuestion = question == null ? "" : question.trim().replaceAll("\\s+", " ").toLowerCase();
        String imageHash = sha256(imageBytes);
        return sha256((sessionId + "|" + normalizedQuestion + "|" + imageHash).getBytes());
    }

    public static String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
