package com.wwzyhy.shopagent.dto;

public record ConversationTurnResponse(
        String type,
        MessageSendResponse guide,
        CartActionResponse cartAction,
        String message,
        MessageResponse userMessage,
        MessageResponse assistantMessage
) {
}