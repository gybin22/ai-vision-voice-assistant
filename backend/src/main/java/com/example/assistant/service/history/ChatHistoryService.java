package com.example.assistant.service.history;

import com.example.assistant.dto.history.*;
import com.example.assistant.entity.history.ChatMessageEntity;
import com.example.assistant.entity.history.ChatSessionEntity;
import com.example.assistant.repository.history.ChatMessageRepository;
import com.example.assistant.repository.history.ChatSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatHistoryService {
    private static final int TITLE_MAX_LENGTH = 40;
    private static final int PREVIEW_MAX_LENGTH = 120;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;

    public ChatHistoryService(
            ChatSessionRepository sessionRepository,
            ChatMessageRepository messageRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
    }

    @Transactional
    public void recordExchange(
            Long userId,
            String sessionId,
            String userQuestion,
            String assistantAnswer,
            String requestId,
            String modelName,
            int inputTokens,
            int outputTokens,
            int totalTokens,
            long chargedTokens
    ) {
        if (userId == null || sessionId == null || sessionId.isBlank()) {
            return;
        }

        Instant now = Instant.now();
        String normalizedQuestion = normalize(userQuestion);
        String normalizedAnswer = normalize(assistantAnswer);

        ChatSessionEntity session = sessionRepository.findByUserIdAndSessionId(userId, sessionId)
                .orElseGet(() -> {
                    ChatSessionEntity created = new ChatSessionEntity();
                    created.setUserId(userId);
                    created.setSessionId(sessionId);
                    created.setTitle(toTitle(normalizedQuestion));
                    created.setMessageCount(0);
                    created.setLastMessageAt(now);
                    return created;
                });

        if (session.getTitle() == null || session.getTitle().isBlank() || "新的对话".equals(session.getTitle())) {
            session.setTitle(toTitle(normalizedQuestion));
        }
        session.setMessageCount(session.getMessageCount() + 2);
        session.setLastMessagePreview(toPreview(normalizedAnswer));
        session.setLastMessageAt(now);
        sessionRepository.save(session);

        ChatMessageEntity userMessage = baseMessage(userId, sessionId, "user", normalizedQuestion);
        userMessage.setRequestId(requestId);
        messageRepository.save(userMessage);

        ChatMessageEntity assistantMessage = baseMessage(userId, sessionId, "assistant", normalizedAnswer);
        assistantMessage.setRequestId(requestId);
        assistantMessage.setModelName(modelName);
        assistantMessage.setInputTokens(Math.max(0, inputTokens));
        assistantMessage.setOutputTokens(Math.max(0, outputTokens));
        assistantMessage.setTotalTokens(totalTokens > 0 ? totalTokens : Math.max(0, inputTokens) + Math.max(0, outputTokens));
        assistantMessage.setChargedTokens(Math.max(0, chargedTokens));
        messageRepository.save(assistantMessage);
    }

    @Transactional(readOnly = true)
    public List<ChatHistoryDayDTO> listGroupedByDay(Long userId) {
        List<ChatSessionEntity> sessions = sessionRepository.findByUserIdOrderByLastMessageAtDesc(userId);
        ZoneId zone = ZoneId.systemDefault();
        Map<LocalDate, List<ChatSessionSummaryDTO>> grouped = new LinkedHashMap<>();

        for (ChatSessionEntity session : sessions) {
            LocalDate date = LocalDateTime.ofInstant(session.getLastMessageAt(), zone).toLocalDate();
            grouped.computeIfAbsent(date, ignored -> new ArrayList<>()).add(toSummary(session));
        }

        List<ChatHistoryDayDTO> result = new ArrayList<>();
        for (Map.Entry<LocalDate, List<ChatSessionSummaryDTO>> entry : grouped.entrySet()) {
            result.add(new ChatHistoryDayDTO(
                    entry.getKey().format(DateTimeFormatter.ISO_LOCAL_DATE),
                    labelFor(entry.getKey(), zone),
                    entry.getValue()
            ));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public ChatSessionDetailDTO getSessionDetail(Long userId, String sessionId) {
        ChatSessionEntity session = sessionRepository.findByUserIdAndSessionId(userId, sessionId)
                .orElseThrow(() -> new IllegalArgumentException("会话不存在或已被删除。"));
        List<ChatHistoryMessageDTO> messages = messageRepository
                .findByUserIdAndSessionIdOrderByCreatedAtAsc(userId, sessionId)
                .stream()
                .map(this::toMessage)
                .toList();
        return new ChatSessionDetailDTO(toSummary(session), messages);
    }

    @Transactional
    public ClearChatHistoryResponse clearAll(Long userId) {
        long messageCount = messageRepository.countByUserId(userId);
        long sessionCount = sessionRepository.countByUserId(userId);
        messageRepository.deleteByUserId(userId);
        sessionRepository.deleteByUserId(userId);
        return new ClearChatHistoryResponse(sessionCount, messageCount);
    }

    @Transactional
    public ClearChatHistoryResponse deleteSession(Long userId, String sessionId) {
        long messageCount = messageRepository.deleteByUserIdAndSessionId(userId, sessionId);
        long sessionCount = sessionRepository.deleteByUserIdAndSessionId(userId, sessionId);
        return new ClearChatHistoryResponse(sessionCount, messageCount);
    }

    private ChatMessageEntity baseMessage(Long userId, String sessionId, String role, String content) {
        ChatMessageEntity message = new ChatMessageEntity();
        message.setUserId(userId);
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content == null ? "" : content);
        message.setInputTokens(0);
        message.setOutputTokens(0);
        message.setTotalTokens(0);
        message.setChargedTokens(0);
        return message;
    }

    private ChatSessionSummaryDTO toSummary(ChatSessionEntity session) {
        return new ChatSessionSummaryDTO(
                session.getSessionId(),
                session.getTitle(),
                session.getMessageCount(),
                session.getLastMessagePreview(),
                session.getCreatedAt(),
                session.getUpdatedAt(),
                session.getLastMessageAt()
        );
    }

    private ChatHistoryMessageDTO toMessage(ChatMessageEntity message) {
        return new ChatHistoryMessageDTO(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getRequestId(),
                message.getModelName(),
                message.getInputTokens(),
                message.getOutputTokens(),
                message.getTotalTokens(),
                message.getChargedTokens(),
                message.getCreatedAt()
        );
    }

    private static String labelFor(LocalDate date, ZoneId zone) {
        LocalDate today = LocalDate.now(zone);
        if (date.equals(today)) {
            return "今天";
        }
        if (date.equals(today.minusDays(1))) {
            return "昨天";
        }
        return date.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String toTitle(String question) {
        String normalized = normalize(question);
        if (normalized.isBlank()) {
            return "新的对话";
        }
        return truncate(normalized, TITLE_MAX_LENGTH);
    }

    private static String toPreview(String value) {
        String normalized = normalize(value).replaceAll("\\s+", " ");
        return truncate(normalized, PREVIEW_MAX_LENGTH);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 1)) + "…";
    }
}
