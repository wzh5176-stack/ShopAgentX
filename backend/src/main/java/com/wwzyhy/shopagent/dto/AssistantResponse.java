package com.wwzyhy.shopagent.dto;

import java.util.List;

public record AssistantResponse(
        String keyword,
        String maxPrice,
        String answer,
        List<ProductSummaryResponse> products
) {
}