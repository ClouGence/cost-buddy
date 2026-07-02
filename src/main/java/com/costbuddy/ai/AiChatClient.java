package com.costbuddy.ai;

import com.costbuddy.domain.AiEngineDO;

public interface AiChatClient {

    String chat(AiEngineDO aiEngine, String systemPrompt, String userPrompt);
}
