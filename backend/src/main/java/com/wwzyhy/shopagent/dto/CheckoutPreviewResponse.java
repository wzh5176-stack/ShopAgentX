package com.wwzyhy.shopagent.dto;

import java.util.List;

public record CheckoutPreviewResponse(
        AddressResponse address,
        List<CheckoutItemResponse> items,
        String totalAmount
) {
    public CheckoutPreviewResponse {
        items = List.copyOf(items);
    }
}