package com.wwzyhy.shopagent.dto;

import java.util.List;

public record ConversationPageResponse(
        List<ConversationResponse> items,
        long page,
        long size,
        long total,
        long totalPages
) {
}