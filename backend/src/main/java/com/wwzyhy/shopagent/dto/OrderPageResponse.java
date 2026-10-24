package com.wwzyhy.shopagent.dto;

import java.util.List;

public record OrderPageResponse(
        List<OrderSummaryResponse> items,
        long page,
        long size,
        long total,
        long pages
) {
    public OrderPageResponse {
        items = List.copyOf(items);
    }
}
