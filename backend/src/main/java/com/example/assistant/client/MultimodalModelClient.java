package com.example.assistant.client;

import com.example.assistant.model.VisionChatCommand;
import com.example.assistant.model.VisionChatResult;

public interface MultimodalModelClient {
    VisionChatResult chat(VisionChatCommand command);
}
