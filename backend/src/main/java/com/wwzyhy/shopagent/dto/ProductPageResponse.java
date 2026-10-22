package com.wwzyhy.shopagent.dto;

import java.util.List;

public record ProductPageResponse(
        List<ProductSummaryResponse> items,
        long page,
        long size,
        long total,
        long totalPages
) {
}