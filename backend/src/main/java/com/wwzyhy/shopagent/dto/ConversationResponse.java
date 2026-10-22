package com.wwzyhy.shopagent.dto;

import java.time.LocalDateTime;

public record ConversationResponse(
        Long id,
        String title,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}