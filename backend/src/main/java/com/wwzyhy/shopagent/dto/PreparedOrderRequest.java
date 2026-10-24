package com.wwzyhy.shopagent.dto;

import java.util.List;

public record PreparedOrderRequest(
        String requestKey,
        Long addressId,
        List<CheckoutItemRequest> items,
        String requestHash
) {
    public PreparedOrderRequest {
        items = List.copyOf(items);
    }
}