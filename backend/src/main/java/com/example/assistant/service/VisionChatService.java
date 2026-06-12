package com.example.assistant.service;

import com.example.assistant.client.MultimodalModelClient;
import com.example.assistant.config.AssistantProperties;
import com.example.assistant.dto.ResponseUsageDTO;
import com.example.assistant.dto.VisionChatResponse;
import com.example.assistant.exception.ModelCallException;
import com.example.assistant.model.CachedVisionAnswer;
import com.example.assistant.model.ChatMessage;
import com.example.assistant.model.VisionChatCommand;
import com.example.assistant.model.VisionChatResult;
import com.example.assistant.util.ImageHashUtil;
import com.example.assistant.util.PromptBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class VisionChatService {
    private final MultimodalModelClient modelClient;
    private final CostControlService costControlService;
    private final ImagePreprocessService imagePreprocessService;
    private final ConversationService conversationService;
    private final CacheService cacheService;
    private final AssistantProperties properties;

    public VisionChatService(
            MultimodalModelClient modelClient,
            CostControlService costControlService,
            ImagePreprocessService imagePreprocessService,
            ConversationService conversationService,
            CacheService cacheService,
            AssistantProperties properties
    ) {
        this.modelClient = modelClient;
        this.costControlService = costControlService;
        this.imagePreprocessService = imagePreprocessService;
        this.conversationService = conversationService;
        this.cacheService = cacheService;
        this.properties = properties;
    }

    public VisionChatResponse chat(
            String sessionId,
            String question,
            MultipartFile image,
            String inputType,
            boolean enableHistory,
            int maxOutputTokens,
            Integer clientImageWidth,
            Integer clientImageHeight,
            String clientIp
    ) {
        Instant start = Instant.now();
        String requestId = "req_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String normalizedQuestion = question == null ? "" : question.trim();
        int boundedMaxOutputTokens = Math.min(maxOutputTokens, properties.getCost().getMaxOutputTokens());

        byte[] imageBytes = readImage(image);
        String mimeType = image.getContentType() == null ? "image/jpeg" : image.getContentType();

        costControlService.validateRequest(sessionId, clientIp, normalizedQuestion, imageBytes.length, boundedMaxOutputTokens);
        imagePreprocessService.validateImage(imageBytes, mimeType, clientImageWidth, clientImageHeight);

        String cacheKey = ImageHashUtil.cacheKey(sessionId, normalizedQuestion, imageBytes);
        CachedVisionAnswer cached = cacheService.get(cacheKey);
        if (cached != null) {
            long latency = Duration.between(start, Instant.now()).toMillis();
            costControlService.recordUsage(sessionId, clientIp, cached.estimatedCost());
            return new VisionChatResponse(
                    requestId,
                    sessionId,
                    cached.answer(),
                    cached.model(),
                    true,
                    new ResponseUsageDTO(cached.inputTokens(), cached.outputTokens(), imageBytes.length, cached.estimatedCost()),
                    latency
            );
        }

        List<ChatMessage> history = enableHistory
                ? conversationService.getRecentMessages(sessionId, properties.getCost().getHistoryRounds())
                : List.of();

        VisionChatCommand command = new VisionChatCommand(
                requestId,
                sessionId,
                normalizedQuestion,
                imageBytes,
                mimeType,
                enableHistory,
                boundedMaxOutputTokens,
                history,
                PromptBuilder.systemPrompt()
        );

        VisionChatResult result = modelClient.chat(command);
        if (result.answer() == null || result.answer().isBlank()) {
            throw new ModelCallException("模型返回空答案", null);
        }

        cacheService.put(cacheKey, new CachedVisionAnswer(
                result.answer(), result.model(), result.inputTokens(), result.outputTokens(), result.estimatedCost()
        ));
        conversationService.append(sessionId, new ChatMessage("user", normalizedQuestion));
        conversationService.append(sessionId, new ChatMessage("assistant", result.answer()));
        costControlService.recordUsage(sessionId, clientIp, result.estimatedCost());

        long latency = Duration.between(start, Instant.now()).toMillis();
        return new VisionChatResponse(
                requestId,
                sessionId,
                result.answer(),
                result.model(),
                false,
                new ResponseUsageDTO(result.inputTokens(), result.outputTokens(), imageBytes.length, result.estimatedCost()),
                latency
        );
    }

    private byte[] readImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("缺少图片文件");
        }
        try {
            return image.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("读取图片失败", e);
        }
    }
}
