package com.wwzyhy.shopagent.dto;

public record CartToolPrepareResponse(
        String action,
        Long conversationId,
        Long sourceMessageId,
        ProductSummaryResponse product,
        Integer quantity,
        String message
) {
}