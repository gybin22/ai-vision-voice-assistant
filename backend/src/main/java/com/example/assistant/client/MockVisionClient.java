package com.example.assistant.client;

import com.example.assistant.model.VisionChatCommand;
import com.example.assistant.model.VisionChatResult;

public class MockVisionClient implements MultimodalModelClient {
    @Override
    public VisionChatResult chat(VisionChatCommand command) {
        String answer = "这是 Mock 模型回答：我已收到你的问题“" + command.question() + "”，"
                + "并收到了当前摄像头截图。接入真实多模态模型后，这里会返回基于画面的视觉理解结果。";
        int estimatedInputTokens = Math.max(50, command.question().length() / 2 + command.imageBytes().length / 300);
        int outputTokens = Math.max(20, answer.length() / 2);
        return new VisionChatResult(answer, "mock-vision-model", estimatedInputTokens, outputTokens, 0.0);
    }
}
