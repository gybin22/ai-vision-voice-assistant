package com.example.assistant.controller;

import com.example.assistant.dto.SessionUsageDTO;
import com.example.assistant.dto.VisionChatResponse;
import com.example.assistant.service.ConversationService;
import com.example.assistant.service.CostControlService;
import com.example.assistant.service.VisionChatService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ChatController {
    private final VisionChatService visionChatService;
    private final CostControlService costControlService;
    private final ConversationService conversationService;

    public ChatController(
            VisionChatService visionChatService,
            CostControlService costControlService,
            ConversationService conversationService
    ) {
        this.visionChatService = visionChatService;
        this.costControlService = costControlService;
        this.conversationService = conversationService;
    }

    @PostMapping(value = "/chat/vision", consumes = "multipart/form-data")
    public VisionChatResponse visionChat(
            @RequestParam @NotBlank String sessionId,
            @RequestParam @NotBlank String question,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestParam(defaultValue = "") String visualSummary,
            @RequestParam(defaultValue = "true") boolean enableHistory,
            @RequestParam(defaultValue = "500") int maxOutputTokens,
            @RequestParam(required = false) Integer clientImageWidth,
            @RequestParam(required = false) Integer clientImageHeight,
            @RequestParam(required = false) String frameMetadata,
            HttpServletRequest request
    ) {
        String clientIp = resolveClientIp(request);
        return visionChatService.chat(
                sessionId,
                question,
                images,
                image,
                visualSummary,
                enableHistory,
                maxOutputTokens,
                clientImageWidth,
                clientImageHeight,
                frameMetadata,
                clientIp
        );
    }

    @GetMapping("/usage/session/{sessionId}")
    public SessionUsageDTO usage(@PathVariable String sessionId) {
        return costControlService.getSessionUsage(sessionId);
    }

    @DeleteMapping("/conversation/{sessionId}")
    public Object clearConversation(@PathVariable String sessionId) {
        conversationService.clear(sessionId);
        return new ClearConversationResponse(sessionId, true);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public record ClearConversationResponse(String sessionId, boolean deleted) {}
}
