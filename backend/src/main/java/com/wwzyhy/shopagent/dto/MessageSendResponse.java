package com.wwzyhy.shopagent.dto;

public record MessageSendResponse(
        MessageResponse userMessage,
        MessageResponse assistantMessage,
        AssistantResponse recommendation
) {
}