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

        String answer = "这是 Mock 模型回答：我已收到你的问题“" + command.question() + "”，"
                + "并收到了按时间顺序排列的 " + frameCount + " 张动作关键帧。"
                + "接入真实多模态模型后，这里会结合这些关键帧分析画面中的动作变化。";
        int estimatedInputTokens = Math.max(50, command.question().length() / 2 + (int) (totalImageBytes / 300));
        int outputTokens = Math.max(20, answer.length() / 2);
        return new VisionChatResult(answer, "mock-vision-model", estimatedInputTokens, outputTokens, 0.0);
    }
}
