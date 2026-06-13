package com.example.assistant.client;

import com.example.assistant.model.VisionChatCommand;
import com.example.assistant.model.VisionChatResult;

public class MockVisionClient implements MultimodalModelClient {
    @Override
    public VisionChatResult chat(VisionChatCommand command) {
        int frameCount = command.frames() == null ? 0 : command.frames().size();
        long totalImageBytes = command.frames() == null
                ? 0
                : command.frames().stream().mapToLong(frame -> frame.bytes().length).sum();

        String answer = switch (command.questionMode()) {
            case "current" -> "这是 Mock 模式。我收到了当前帧，接入真实模型后会只回答你问到的当前画面内容。";
            case "motion" -> "这是 Mock 模式。我收到了最近动作事件的代表帧，接入真实模型后会根据动作变化回答。";
            case "detailed" -> "这是 Mock 模式。我收到了更多视觉证据，接入真实模型后会做更完整的视觉分析。";
            default -> "这是 Mock 模式。本轮是普通聊天，没有上传图片；接入真实模型后会像普通对话一样回答。";
        };
        int estimatedInputTokens = Math.max(50, command.question().length() / 2 + (int) (totalImageBytes / 300));
        int outputTokens = Math.max(20, answer.length() / 2);
        return new VisionChatResult(answer + " 当前模式：" + command.questionMode() + "，图片数：" + frameCount + "。", "mock-vision-model", estimatedInputTokens, outputTokens, 0.0);
    }
}
