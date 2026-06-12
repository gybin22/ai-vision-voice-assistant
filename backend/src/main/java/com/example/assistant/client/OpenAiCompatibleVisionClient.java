package com.example.assistant.client;

import com.example.assistant.config.AssistantProperties;
import com.example.assistant.exception.ModelCallException;
import com.example.assistant.model.ChatMessage;
import com.example.assistant.model.VisionChatCommand;
import com.example.assistant.model.VisionChatResult;
import com.example.assistant.model.VisionFrame;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OpenAiCompatibleVisionClient implements MultimodalModelClient {
    private final RestClient restClient;
    private final AssistantProperties properties;

    public OpenAiCompatibleVisionClient(RestClient restClient, AssistantProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public VisionChatResult chat(VisionChatCommand command) {
        String apiKey = properties.getModel().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new ModelCallException("未配置模型 API Key。请设置 DASHSCOPE_API_KEY 或 ASSISTANT_MODEL_API_KEY。", null);
        }

        if (command.frames() == null || command.frames().isEmpty()) {
            throw new ModelCallException("没有可发送给模型的关键帧", null);
        }

        String url = trimRightSlash(properties.getModel().getBaseUrl()) + "/chat/completions";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getModel().getModelName());
        body.put("max_tokens", command.maxOutputTokens());
        body.put("temperature", 0.2);
        body.put("stream", false);
        body.put("messages", buildMessages(command));

        try {
            Map<?, ?> response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                throw new ModelCallException("模型响应为空", null);
            }

            String answer = extractAnswer(response);
            Usage usage = extractUsage(response);
            double estimatedCost = estimateCost(usage.inputTokens(), usage.outputTokens());

            return new VisionChatResult(
                    answer,
                    properties.getModel().getModelName(),
                    usage.inputTokens(),
                    usage.outputTokens(),
                    estimatedCost
            );
        } catch (RestClientResponseException e) {
            String responseBody = e.getResponseBodyAsString();
            String detail = responseBody == null || responseBody.isBlank()
                    ? e.getMessage()
                    : responseBody;
            throw new ModelCallException("调用多模态模型失败，HTTP " + e.getStatusCode() + "：" + detail, e);
        } catch (Exception e) {
            if (e instanceof ModelCallException mce) {
                throw mce;
            }
            throw new ModelCallException("调用多模态模型失败：" + e.getMessage(), e);
        }
    }

    private List<Map<String, Object>> buildMessages(VisionChatCommand command) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", command.systemPrompt()));

        if (command.enableHistory()) {
            for (ChatMessage item : command.history()) {
                messages.add(Map.of("role", item.role(), "content", item.content()));
            }
        }

        List<Map<String, Object>> userContent = new ArrayList<>();
        userContent.add(Map.of(
                "type", "text",
                "text", buildUserText(command)
        ));

        for (VisionFrame frame : command.frames()) {
            userContent.add(Map.of(
                    "type", "text",
                    "text", "关键帧 #" + frame.sequence() + "，文件名：" + frame.filename()
            ));
            userContent.add(Map.of(
                    "type", "image_url",
                    "image_url", Map.of("url", toDataUrl(frame))
            ));
        }

        messages.add(Map.of("role", "user", "content", userContent));
        return messages;
    }

    private String buildUserText(VisionChatCommand command) {
        StringBuilder text = new StringBuilder();
        text.append("用户问题：").append(command.question()).append("\n\n")
                .append("下面有 ").append(command.frames().size()).append(" 张关键帧，均来自同一个摄像头，按时间顺序排列。")
                .append("请把它们当作一个短动作片段来理解，重点分析前后变化，而不是只看最后一张图。\n");

        if (command.frameMetadataJson() != null && !command.frameMetadataJson().isBlank()) {
            text.append("\n前端关键帧元数据 JSON：")
                    .append(command.frameMetadataJson())
                    .append("\n");
        }

        return text.toString();
    }

    private String toDataUrl(VisionFrame frame) {
        String base64Image = Base64.getEncoder().encodeToString(frame.bytes());
        return "data:" + frame.mimeType() + ";base64," + base64Image;
    }

    private String extractAnswer(Map<?, ?> response) {
        Object choicesObj = response.get("choices");
        if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) {
            throw new ModelCallException("模型响应中没有 choices", null);
        }

        Object firstChoice = choices.get(0);
        if (!(firstChoice instanceof Map<?, ?> choice)) {
            throw new ModelCallException("模型响应 choices 格式异常", null);
        }

        Object messageObj = choice.get("message");
        if (!(messageObj instanceof Map<?, ?> message)) {
            throw new ModelCallException("模型响应 message 格式异常", null);
        }

        Object content = message.get("content");
        if (content == null) {
            return "模型没有返回文本内容。";
        }
        return content.toString().trim();
    }

    private Usage extractUsage(Map<?, ?> response) {
        Object usageObj = response.get("usage");
        if (!(usageObj instanceof Map<?, ?> usage)) {
            return new Usage(0, 0);
        }
        int input = toInt(usage.get("prompt_tokens"));
        int output = toInt(usage.get("completion_tokens"));
        return new Usage(input, output);
    }

    private int toInt(Object value) {
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private double estimateCost(int inputTokens, int outputTokens) {
        // MVP 只做估算。真实价格应按阿里云百炼 qwen3-vl-plus 价格表配置。
        return (inputTokens / 1_000_000.0) * 0.15 + (outputTokens / 1_000_000.0) * 0.60;
    }

    private String trimRightSlash(String value) {
        if (value == null || value.isBlank()) return "";
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private record Usage(int inputTokens, int outputTokens) {}
}
