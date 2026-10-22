package com.wwzyhy.shopagent.dto;

public record AiConversationIntentData(
        String keyword,
        String maxPrice,
        String clarification
) {
}