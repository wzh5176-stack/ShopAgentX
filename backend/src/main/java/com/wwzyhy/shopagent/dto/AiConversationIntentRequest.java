package com.wwzyhy.shopagent.dto;

import java.util.List;

public record AiConversationIntentRequest(
        String question,
        List<AiConversationMessage> history
) {
}