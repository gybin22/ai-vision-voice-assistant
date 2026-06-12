package com.example.assistant.util;

public final class PromptBuilder {
    private PromptBuilder() {}

    public static String systemPrompt() {
        return "你是一个视觉对话助手。请根据用户问题和图片内容回答。"
                + "要求：1）只描述图片中能明确看到的内容；"
                + "2）如果无法确定，请说明“不确定”；"
                + "3）回答简洁、自然，适合语音播报；"
                + "4）不要编造画面中不存在的细节；"
                + "5）如果用户要求安全、医疗、法律等高风险判断，请提醒用户谨慎核实。";
    }
}
