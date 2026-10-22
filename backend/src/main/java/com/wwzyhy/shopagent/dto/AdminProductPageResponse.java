package com.wwzyhy.shopagent.dto;

import java.util.List;

public record AdminProductPageResponse(
        List<AdminProductSummaryResponse> items,
        long page,
        long size,
        long total,
        long totalPages
) {
}