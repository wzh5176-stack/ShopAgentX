package com.wwzyhy.shopagent.dto;

import java.time.LocalDateTime;

public record CartActionResponse(
        String operationId,
        Long conversationId,
        Long sourceMessageId,
        Long productId,
        String title,
        Integer baseQuantity,
        Integer quantity,
        CartActionStatus status,
        LocalDateTime expiresAt,
        CartItemSetResponse result
) {
}