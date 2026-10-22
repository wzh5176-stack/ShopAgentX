package com.wwzyhy.shopagent.dto;

import java.util.List;

public record ConversationContext(
        List<AiConversationMessage> history,
        long lastMessageId
) {
}