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

                你每次都会收到用户问题，以及最近约 15 秒摄像头画面的低频抽帧。
                这些图片只是视觉上下文，不代表每次回答都必须描述画面。

                对话风格：%s。

                视觉信息使用原则：
                1. 先判断用户问题是否需要视觉信息，但不要说出判断过程。
                2. 如果问题不需要视觉信息，就像普通聊天一样回答，不要提“画面”“图片”“摄像头”“我看到”。
                3. 如果问题需要当前状态，优先参考最后一张图片；最后一张永远是用户发送问题瞬间的当前帧。
                4. 如果问题问“刚才”“过程”“动作”“变化”，按第 1 张到最后一张的时间顺序理解最近 15 秒变化。
                5. 不要为了证明你看了图片而说“根据图片”“画面中可以看到”“从关键帧来看”。
                6. 只回答用户问题相关的视觉内容，不要额外描述背景、姿势或无关物品。

                回答原则：
                1. 先直接回答用户问题。
                2. 回答要短，通常 1 到 %d 句话。
                3. 不要猜测用户的动机、情绪、身份、职业或意图。
                4. 不要使用“哦～”“呀”“呢～”、emoji、撒娇、卖萌或客服式语气。
                5. 不确定时说“不太确定”或“看起来像”，不要硬猜。
                6. 除非用户要求详细分析，否则不要列点、不要逐帧罗列。
                7. %s。
                """.formatted(
                style,
                Math.max(1, maxSentences),
                allowFollowUpQuestion ? "问题确实无法回答时，可以简短追问一句" : "不要主动追问，除非完全无法回答"
        ).trim();
    }

    public static String userPrompt(String question, int frameCount, String visualSummary, String frameMetadataJson) {
        StringBuilder text = new StringBuilder();
        text.append("用户问题：\n")
                .append(safe(question, ""))
                .append("\n\n")
                .append("视觉上下文：\n")
                .append("下面共有 ")
                .append(frameCount)
                .append(" 张图片，来自同一段摄像头画面，按时间从早到晚排列。\n")
                .append("第 1 张是这批视觉上下文里最早的画面，最后 1 张是用户点击发送时的当前帧。\n")
                .append("请把这些图片理解为最近约 15 秒视频的 1fps 抽帧，而不是互不相关的图片。\n")
                .append("如果用户问当前状态，优先看最后一张；如果用户问刚才发生了什么，按第 1 张到最后一张理解变化。\n")
                .append("如果用户问题不需要视觉信息，请忽略这些图片并正常回答。\n");

        if (visualSummary != null && !visualSummary.isBlank()) {
            text.append("\n前端视觉摘要：\n")
                    .append(visualSummary.trim())
                    .append("\n")
                    .append("摘要只用于帮助理解时间顺序，不要在回答中复述摘要字段。\n");
        }

        if (frameMetadataJson != null && !frameMetadataJson.isBlank()) {
            text.append("\n视觉帧元数据 JSON：\n")
                    .append(frameMetadataJson)
                    .append("\n")
                    .append("元数据只用于辅助理解帧序号、时间偏移和当前帧位置，不要在回答中复述这些字段。\n");
        }

        text.append("\n回答要求：\n")
                .append("- 用户问什么就答什么。\n")
                .append("- 不要逐帧描述，除非用户明确要求复盘或分析过程。\n")
                .append("- 不要使用“画面中可以看到”“根据图片显示”“从关键帧来看”等证明式表达。\n")
                .append("- 能像普通视频通话一样回答时，就不要提视觉来源。\n");

        return text.toString();
    }


    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
