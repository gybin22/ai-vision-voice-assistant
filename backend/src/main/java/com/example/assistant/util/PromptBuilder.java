package com.example.assistant.util;

import com.example.assistant.config.AssistantProperties;

public final class PromptBuilder {
    private PromptBuilder() {}

    public static String systemPrompt(AssistantProperties.Dialogue dialogue) {
        int maxSentences = dialogue == null ? 2 : dialogue.getMaxSentences();
        boolean allowFollowUpQuestion = dialogue != null && dialogue.isAllowFollowUpQuestion();
        String style = dialogue == null ? "calm-natural" : safe(dialogue.getStyle(), "calm-natural");

        return """
                你是一个正在和用户视频通话的 AI 助手。

                你的目标不是每次描述画面，而是像正常人一样对话。摄像头画面只是你的上下文之一，
                不是每次回答都必须提到。

                对话风格：%s。

                视觉信息使用原则：
                1. 回答前先判断用户的问题是否真的需要视觉信息，但不要说出判断过程。
                2. 如果用户的问题不依赖画面，就正常聊天，不要提“画面”“图片”“我看到”“摄像头”。
                3. 如果用户的问题依赖画面，只回答与问题直接相关的内容。
                4. 不要为了证明你能看见而反复使用“我看到”“画面里”“根据图片”“从关键帧来看”。
                5. 不要额外描述用户、背景、姿势或无关物品。

                回答原则：
                1. 先直接回答用户问题。
                2. 回答要短，通常 1 到 %d 句话。
                3. 不要猜测用户的动机、情绪、身份、职业或意图。
                4. 不要使用“哦～”“呀”“呢～”、emoji、撒娇、卖萌或客服式语气。
                5. 不确定时说“不太确定”或“看起来像”，不要硬猜。
                6. 除非用户要求详细分析，否则不要列点、不要长篇解释。
                7. %s。

                问题模式：
                - chat：普通聊天，禁止主动使用视觉信息。
                - current：当前画面问题，只看当前帧，只回答用户问到的对象或状态。
                - motion：动作变化问题，把图片理解为动作事件代表帧，只描述相关变化。
                - detailed：详细视觉分析，可以更充分使用视觉证据，但仍不要机械描述。
                """.formatted(
                style,
                Math.max(1, maxSentences),
                allowFollowUpQuestion ? "问题确实无法回答时，可以简短追问一句" : "不要主动追问，除非完全无法回答"
        ).trim();
    }

    public static String userPrompt(String question, String questionMode, int frameCount, String visualSummary, String frameMetadataJson) {
        String mode = safe(questionMode, "chat");
        StringBuilder text = new StringBuilder();
        text.append("用户问题：\n")
                .append(safe(question, ""))
                .append("\n\n")
                .append("本轮问题模式：")
                .append(mode)
                .append("\n");

        switch (mode) {
            case "current" -> text.append("本轮只提供当前帧。请只回答用户问到的当前对象或状态，不要展开成画面描述。\n");
            case "motion" -> text.append("本轮提供最近视觉事件的代表帧。请把它们理解为一段动作变化，只回答用户问到的变化。\n");
            case "detailed" -> text.append("本轮提供较多视觉证据。可以更仔细分析，但不要复述无关背景或元数据。\n");
            default -> text.append("本轮没有上传图片。请按普通聊天回答，不要声称看到了画面。\n");
        }

        if (frameCount > 0) {
            text.append("\n视觉证据：共 ")
                    .append(frameCount)
                    .append(" 张图片，按时间顺序排列。\n");
        }

        if (visualSummary != null && !visualSummary.isBlank()) {
            text.append("\n前端视觉摘要：\n")
                    .append(visualSummary.trim())
                    .append("\n")
                    .append("摘要只用于帮助理解是否需要视觉信息，不要在回答中复述摘要字段。\n");
        }

        if (frameMetadataJson != null && !frameMetadataJson.isBlank()) {
            text.append("\n视觉帧元数据 JSON：\n")
                    .append(frameMetadataJson)
                    .append("\n")
                    .append("元数据只用于辅助理解时间顺序和变化强度，不要在回答中复述这些字段。\n");
        }

        text.append("\n回答要求：\n")
                .append("- 用户问什么就答什么。\n")
                .append("- 普通聊天不要提画面。\n")
                .append("- 视觉问题只回答相关内容，不要主动扩展。\n")
                .append("- 不要使用“画面中可以看到”“根据图片显示”“从关键帧来看”等证明式表达。\n");

        return text.toString();
    }

    public static String visualContextPrompt(String question, int frameCount, String frameMetadataJson) {
        return userPrompt(question, "motion", frameCount, "", frameMetadataJson);
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
