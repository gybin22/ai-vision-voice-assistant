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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class VisionChatService {
    private final MultimodalModelClient modelClient;
    private final CostControlService costControlService;
    private final ImagePreprocessService imagePreprocessService;
    private final ConversationService conversationService;
    private final CacheService cacheService;
    private final AssistantProperties properties;
    private final ObjectMapper objectMapper;

    public VisionChatService(
            MultimodalModelClient modelClient,
            CostControlService costControlService,
            ImagePreprocessService imagePreprocessService,
            ConversationService conversationService,
            CacheService cacheService,
            AssistantProperties properties,
            ObjectMapper objectMapper
    ) {
        this.modelClient = modelClient;
        this.costControlService = costControlService;
        this.imagePreprocessService = imagePreprocessService;
        this.conversationService = conversationService;
        this.cacheService = cacheService;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public VisionChatResponse chat(
            String sessionId,
            String question,
            List<MultipartFile> images,
            MultipartFile legacyImage,
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
        String normalizedVisualSummary = visualSummary == null ? "" : visualSummary.trim();
        int boundedMaxOutputTokens = Math.min(maxOutputTokens, properties.getCost().getMaxOutputTokens());

        List<VisionFrame> frames = readFrames(images, legacyImage, frameMetadataJson);
        long totalImageBytes = frames.stream().mapToLong(frame -> frame.bytes().length).sum();

        costControlService.validateRequest(
                sessionId,
                clientIp,
                normalizedQuestion,
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

        String cacheKey = ImageHashUtil.cacheKey(sessionId, normalizedQuestion, normalizedVisualSummary, frames);
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

    private List<VisionFrame> readFrames(List<MultipartFile> images, MultipartFile legacyImage, String frameMetadataJson) {
        List<MultipartFile> source = new ArrayList<>();
        if (images != null) {
            source.addAll(images.stream().filter(file -> file != null && !file.isEmpty()).toList());
        }
        if (source.isEmpty() && legacyImage != null && !legacyImage.isEmpty()) {
            source.add(legacyImage);
        }

        if (source.isEmpty()) {
            throw new IllegalArgumentException("缺少视觉帧。当前版本每次提问都需要上传最近 15 秒视觉上下文。");
        }

        if (source.size() > properties.getCost().getMaxFrameCount()) {
            throw new IllegalArgumentException("视觉帧数量过多，最多允许 " + properties.getCost().getMaxFrameCount() + " 张。");
        }

        Map<Integer, FrameMeta> metadataBySequence = parseFrameMetadata(frameMetadataJson);
        List<VisionFrame> frames = new ArrayList<>();
        for (int i = 0; i < source.size(); i += 1) {
            MultipartFile file = source.get(i);
            int sequence = i + 1;
            FrameMeta meta = metadataBySequence.getOrDefault(sequence, FrameMeta.empty(sequence));
            String mimeType = file.getContentType() == null ? "image/jpeg" : file.getContentType();
            String filename = file.getOriginalFilename() == null ? "rolling-frame-" + sequence + ".jpg" : file.getOriginalFilename();
            try {
                frames.add(new VisionFrame(
                        sequence,
                        filename,
                        file.getBytes(),
                        mimeType,
                        meta.role(),
                        meta.capturedAt(),
                        meta.offsetMs(),
                        meta.width(),
                        meta.height(),
                        meta.size()
                ));
            } catch (IOException e) {
                throw new IllegalArgumentException("读取第 " + sequence + " 张视觉帧失败", e);
            }
        }
        return frames;
    }

    private Map<Integer, FrameMeta> parseFrameMetadata(String frameMetadataJson) {
        Map<Integer, FrameMeta> result = new HashMap<>();
        if (frameMetadataJson == null || frameMetadataJson.isBlank()) {
            return result;
        }

        try {
            JsonNode root = objectMapper.readTree(frameMetadataJson);
            if (!root.isArray()) {
                return result;
            }
            for (JsonNode node : root) {
                JsonNode sequenceNode = node.get("sequence");
                int sequence = sequenceNode != null && sequenceNode.canConvertToInt()
                        ? sequenceNode.asInt()
                        : result.size() + 1;
                JsonNode roleNode = node.get("role");
                String role = roleNode == null || roleNode.isNull() || roleNode.asText().isBlank()
                        ? frameRoleFallback(sequence, root.size())
                        : roleNode.asText();
                result.put(sequence, new FrameMeta(
                        sequence,
                        role,
                        longValueOrNull(node, "capturedAt"),
                        longValueOrNull(node, "offsetMs"),
                        intValueOrNull(node, "width"),
                        intValueOrNull(node, "height"),
                        longValueOrNull(node, "size")
                ));
            }
        } catch (Exception ignored) {
            // metadata 只用于 prompt 辅助；解析失败时仍按 multipart 顺序处理图片。
        }
        return result;
    }


    private static String frameRoleFallback(int sequence, int totalFrames) {
        return sequence == totalFrames ? "current" : "history";
    }


    private static Integer intValueOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.canConvertToInt() ? value.asInt() : null;
    }

    private static Long longValueOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.canConvertToLong() ? value.asLong() : null;
    }

    private record FrameMeta(
            int sequence,
            String role,
            Long capturedAt,
            Long offsetMs,
            Integer width,
            Integer height,
            Long size
    ) {
        private static FrameMeta empty(int sequence) {
            return new FrameMeta(sequence, "history", null, null, null, null, null);
        }
    }
}
