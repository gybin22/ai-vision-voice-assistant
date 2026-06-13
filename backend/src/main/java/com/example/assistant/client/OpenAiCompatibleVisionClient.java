package com.example.assistant.client;

import com.example.assistant.config.AssistantProperties;
import com.example.assistant.exception.ModelCallException;
import com.example.assistant.model.ChatMessage;
import com.example.assistant.model.VisionChatCommand;
import com.example.assistant.model.VisionChatResult;
import com.example.assistant.model.VisionFrame;
import com.example.assistant.util.PromptBuilder;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.*;

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

        String url = trimRightSlash(properties.getModel().getBaseUrl()) + "/chat/completions";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getModel().getModelName());
        body.put("max_tokens", command.maxOutputTokens());
        body.put("temperature", properties.getDialogue().getTemperature());
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
            String detail = responseBody.isBlank()
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

        if (command.frames() != null) {
            for (VisionFrame frame : command.frames()) {
                userContent.add(Map.of(
                        "type", "text",
                        "text", frameLabel(frame, command.frames().size())
                ));
                userContent.add(Map.of(
                        "type", "image_url",
                        "image_url", Map.of("url", toDataUrl(frame))
                ));
            }
        }

        messages.add(Map.of("role", "user", "content", userContent));
        return messages;
    }

    private String buildUserText(VisionChatCommand command) {
        int frameCount = command.frames() == null ? 0 : command.frames().size();
        return PromptBuilder.userPrompt(
                command.question(),
                frameCount,
                command.visualSummary(),
                command.frameMetadataJson()
        );
    }


    private String frameLabel(VisionFrame frame, int totalFrames) {
        String role = frame.role() == null || frame.role().isBlank() ? "history" : frame.role();
        String roleLabel = switch (role) {
            case "current" -> "当前帧/最新";
            case "manual" -> "手动补充帧";
            default -> "历史帧";
        };
        String offset = frame.offsetMs() == null ? "未知" : frame.offsetMs() + "ms";
        String size = frame.width() != null && frame.height() != null
                ? frame.width() + "x" + frame.height()
                : "未知尺寸";
        return "帧 " + frame.sequence() + "/" + totalFrames
                + "，" + roleLabel
                + "，相对当前帧时间偏移 t=" + offset
                + "，尺寸 " + size
                + "。这些帧按时间顺序排列，最后一帧是用户发送问题时的当前画面。";
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
