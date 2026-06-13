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

        String answer = "我已经收到你的问题了，也拿到了这段动作里的 " + frameCount
                + " 张关键帧。现在还是 Mock 模式，所以我不能真正看懂画面；接入真实多模态模型后，这里会结合这些关键帧分析画面中的动作变化。";
        int estimatedInputTokens = Math.max(50, command.question().length() / 2 + (int) (totalImageBytes / 300));
        int outputTokens = Math.max(20, answer.length() / 2);
        return new VisionChatResult(answer, "mock-vision-model", estimatedInputTokens, outputTokens, 0.0);
    }
}
