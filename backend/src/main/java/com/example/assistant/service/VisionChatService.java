package com.example.assistant.service;

import com.example.assistant.client.MultimodalModelClient;
import com.example.assistant.config.AssistantProperties;
import com.example.assistant.dto.ResponseUsageDTO;
import com.example.assistant.dto.VisionChatResponse;
import com.example.assistant.exception.CostLimitExceededException;
import com.example.assistant.exception.ModelCallException;
import com.example.assistant.model.*;
import com.example.assistant.util.ImageHashUtil;
import com.example.assistant.util.PromptBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
            List<MultipartFile> images,
            MultipartFile legacyImage,
            String inputType,
            String questionMode,
            String visualSummary,
            boolean enableHistory,
            int maxOutputTokens,
            Integer clientImageWidth,
            Integer clientImageHeight,
            String frameMetadataJson,
            String clientIp
    ) {
        Instant start = Instant.now();
        String requestId = "req_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String normalizedQuestion = question == null ? "" : question.trim();
        String normalizedQuestionMode = normalizeQuestionMode(questionMode);
        String normalizedVisualSummary = visualSummary == null ? "" : visualSummary.trim();
        int boundedMaxOutputTokens = Math.min(maxOutputTokens, properties.getCost().getMaxOutputTokens());

        List<VisionFrame> frames = readFrames(images, legacyImage, normalizedQuestionMode);
        long totalImageBytes = frames.stream().mapToLong(frame -> frame.bytes().length).sum();

        costControlService.validateRequest(
                sessionId,
                clientIp,
                normalizedQuestion,
                normalizedQuestionMode,
                frames.size(),
                totalImageBytes,
                boundedMaxOutputTokens
        );

        for (VisionFrame frame : frames) {
            if (frame.bytes().length > properties.getCost().getMaxImageBytes()) {
                throw new CostLimitExceededException("IMAGE_TOO_LARGE", "第 " + frame.sequence() + " 张视觉帧过大，请降低截图质量后重试。");
            }
            imagePreprocessService.validateImage(frame.bytes(), frame.mimeType(), clientImageWidth, clientImageHeight);
        }

        String cacheKey = ImageHashUtil.cacheKey(sessionId, normalizedQuestion, normalizedQuestionMode, normalizedVisualSummary, frames);
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
                    new ResponseUsageDTO(cached.inputTokens(), cached.outputTokens(), totalImageBytes, cached.estimatedCost()),
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
                normalizedQuestionMode,
                normalizedVisualSummary,
                frames,
                frameMetadataJson == null ? "" : frameMetadataJson,
                enableHistory,
                boundedMaxOutputTokens,
                history,
                PromptBuilder.systemPrompt(properties.getDialogue())
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
                new ResponseUsageDTO(result.inputTokens(), result.outputTokens(), totalImageBytes, result.estimatedCost()),
                latency
        );
    }

    private List<VisionFrame> readFrames(List<MultipartFile> images, MultipartFile legacyImage, String questionMode) {
        List<MultipartFile> source = new ArrayList<>();
        if (images != null) {
            source.addAll(images.stream().filter(file -> file != null && !file.isEmpty()).toList());
        }
        if (source.isEmpty() && legacyImage != null && !legacyImage.isEmpty()) {
            source.add(legacyImage);
        }

        if (source.isEmpty()) {
            if ("chat".equalsIgnoreCase(questionMode)) {
                return List.of();
            }
            throw new IllegalArgumentException("缺少图片文件");
        }

        if (source.size() > properties.getCost().getMaxFrameCount()) {
            throw new IllegalArgumentException("视觉帧数量过多，最多允许 " + properties.getCost().getMaxFrameCount() + " 张。");
        }

        List<VisionFrame> frames = new ArrayList<>();
        for (int i = 0; i < source.size(); i += 1) {
            MultipartFile file = source.get(i);
            String mimeType = file.getContentType() == null ? "image/jpeg" : file.getContentType();
            String filename = file.getOriginalFilename() == null ? "vision-frame-" + (i + 1) + ".jpg" : file.getOriginalFilename();
            try {
                frames.add(new VisionFrame(i + 1, filename, file.getBytes(), mimeType));
            } catch (IOException e) {
                throw new IllegalArgumentException("读取第 " + (i + 1) + " 张视觉帧失败", e);
            }
        }
        return frames;
    }

    private String normalizeQuestionMode(String questionMode) {
        if (questionMode == null || questionMode.isBlank()) {
            return "chat";
        }
        String normalized = questionMode.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "current", "motion", "detailed" -> normalized;
            default -> "chat";
        };
    }
}
