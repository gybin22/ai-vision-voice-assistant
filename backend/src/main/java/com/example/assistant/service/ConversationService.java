package com.example.assistant.service;

import com.example.assistant.model.ChatMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConversationService {
    private static final int MAX_STORED_MESSAGES = 20;
    private final Map<String, Deque<ChatMessage>> conversations = new ConcurrentHashMap<>();

    public void append(String sessionId, ChatMessage message) {
        Deque<ChatMessage> deque = conversations.computeIfAbsent(sessionId, key -> new ArrayDeque<>());
        synchronized (deque) {
            deque.addLast(message);
            while (deque.size() > MAX_STORED_MESSAGES) {
                deque.removeFirst();
            }
        }
    }

    public List<ChatMessage> getRecentMessages(String sessionId, int rounds) {
        Deque<ChatMessage> deque = conversations.get(sessionId);
        if (deque == null || rounds <= 0) return List.of();

        int maxMessages = rounds * 2;
        synchronized (deque) {
            List<ChatMessage> all = new ArrayList<>(deque);
            int fromIndex = Math.max(0, all.size() - maxMessages);
            return all.subList(fromIndex, all.size());
        }
    }

    public void clear(String sessionId) {
        conversations.remove(sessionId);
    }
}
