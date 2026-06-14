package com.example.assistant.controller;

import com.example.assistant.dto.history.ChatHistoryDayDTO;
import com.example.assistant.dto.history.ChatSessionDetailDTO;
import com.example.assistant.dto.history.ClearChatHistoryResponse;
import com.example.assistant.security.UserPrincipal;
import com.example.assistant.service.history.ChatHistoryService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/history")
public class ChatHistoryController {
    private final ChatHistoryService chatHistoryService;

    public ChatHistoryController(ChatHistoryService chatHistoryService) {
        this.chatHistoryService = chatHistoryService;
    }

    @GetMapping
    public List<ChatHistoryDayDTO> list(@AuthenticationPrincipal UserPrincipal principal) {
        return chatHistoryService.listGroupedByDay(principal.getId());
    }

    @GetMapping("/sessions/{sessionId}")
    public ChatSessionDetailDTO detail(
            @PathVariable String sessionId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return chatHistoryService.getSessionDetail(principal.getId(), sessionId);
    }

    @DeleteMapping
    public ClearChatHistoryResponse clearAll(@AuthenticationPrincipal UserPrincipal principal) {
        return chatHistoryService.clearAll(principal.getId());
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ClearChatHistoryResponse deleteSession(
            @PathVariable String sessionId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return chatHistoryService.deleteSession(principal.getId(), sessionId);
    }
}
