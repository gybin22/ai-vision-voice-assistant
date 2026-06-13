package com.example.assistant.util;

import com.example.assistant.config.AssistantProperties;

public final class PromptBuilder {
    private PromptBuilder() {}

    public static String systemPrompt(AssistantProperties.Dialogue dialogue) {
        int maxSentences = dialogue == null ? 4 : dialogue.getMaxSentences();
        boolean allowFollowUpQuestion = dialogue == null || dialogue.isAllowFollowUpQuestion();
        String style = dialogue == null ? "natural" : safe(dialogue.getStyle(), "natural");

        return """
                你是一个自然、友好、简洁的 AI 视觉语音对话助手，正在通过摄像头和用户进行近实时视频交流。
                你的回答应该像真人视频聊天中的回应，而不是检测报告、监控分析报告或说明书。

                对话风格：%s。
                回答原则：
                1. 先自然回应用户，再结合画面说明重点。
                2. 口语化、短句优先，适合直接语音播报。
                3. 不要反复使用“根据图片显示”“画面中可以看到”“从图片可以看出”这类机械表达。
                4. 如果用户问“刚才”“发生了什么”“我做了什么”“变化在哪里”，优先描述关键帧里的动作变化。
                5. 如果用户问“现在”“当前”“这是什么”，优先看最后一帧中明确可见的内容。
                6. 只说能确定的内容；看不清或不确定时，直接说“不太确定”。
                7. 不要编造画面中不存在的细节，也不要过度推断用户意图。
                8. 普通回答控制在 1 到 %d 句话；除非用户要求详细解释。
                9. 如果用户的问题不清楚，%s。
                10. 涉及医疗、法律、安全、财务等高风险判断时，给出谨慎提醒，不要替用户做最终决定。
                """.formatted(
                style,
                Math.max(1, maxSentences),
                allowFollowUpQuestion ? "可以简短追问一句" : "不要主动追问，直接给出最稳妥的回答"
        ).trim();
    }

    public static String visualContextPrompt(String question, int frameCount, String frameMetadataJson) {
        StringBuilder text = new StringBuilder();
        text.append("用户问题：\n")
                .append(safe(question, ""))
                .append("\n\n")
                .append("视觉上下文：\n")
                .append("下面的图片来自同一个摄像头，是按时间顺序自动保存的关键帧，共 ")
                .append(frameCount)
                .append(" 张。\n")
                .append("请把这些关键帧理解为同一段短动作过程，而不是几张互不相关的图片。\n")
                .append("请重点观察：人物姿态、手部动作、物体拿起或放下、物体位置变化、镜头移动、画面前后差异。\n")
                .append("如果用户问动作变化，请按时间顺序说清楚；如果用户问当前画面，请重点参考最后一帧。\n");

        if (frameMetadataJson != null && !frameMetadataJson.isBlank()) {
            text.append("\n关键帧元数据 JSON：\n")
                    .append(frameMetadataJson)
                    .append("\n")
                    .append("元数据只用于辅助理解时间顺序和变化强度，不要在回答中机械复述这些字段。\n");
        }

        text.append("\n回答时请像正在和用户视频聊天一样自然。直接回答用户真正想知道的内容。\n");
        return text.toString();
    }


    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
