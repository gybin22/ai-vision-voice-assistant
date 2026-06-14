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

        String answer = "这是 Mock 模式。我收到了最近 15 秒的滚动视觉上下文，接入真实模型后会自行判断是否需要结合这些帧回答。";
        int estimatedInputTokens = Math.max(50, command.question().length() / 2 + (int) (totalImageBytes / 300));
        int outputTokens = Math.max(20, answer.length() / 2);
        return new VisionChatResult(answer + " 图片数：" + frameCount + "。", "mock-vision-model", estimatedInputTokens, outputTokens, estimatedInputTokens + outputTokens, 0.0);
    }
}
