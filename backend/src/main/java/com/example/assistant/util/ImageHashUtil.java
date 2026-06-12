package com.example.assistant.util;

import com.example.assistant.model.VisionFrame;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

public final class ImageHashUtil {
    private ImageHashUtil() {}

    public static String cacheKey(String sessionId, String question, List<VisionFrame> frames) {
        String normalizedQuestion = normalizeQuestion(question);
        StringBuilder builder = new StringBuilder(sessionId)
                .append('|')
                .append(normalizedQuestion)
                .append('|')
                .append(frames.size());

        for (VisionFrame frame : frames) {
            builder.append('|')
                    .append(frame.sequence())
                    .append(':')
                    .append(frame.mimeType())
                    .append(':')
                    .append(sha256(frame.bytes()));
        }
        return sha256(builder.toString().getBytes());
    }

    public static String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    private static String normalizeQuestion(String question) {
        return question == null ? "" : question.trim().replaceAll("\\s+", " ").toLowerCase();
    }
}
