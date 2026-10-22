package com.wwzyhy.shopagent.dto;

import java.time.LocalDateTime;

public record MessageResponse(
        Long id,
        String role,
        String content,
        LocalDateTime createdAt,
        AssistantResponse recommendation,
        String messageType,
        String cartOperationId
) {
}