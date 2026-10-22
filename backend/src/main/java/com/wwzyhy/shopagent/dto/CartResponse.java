package com.wwzyhy.shopagent.dto;

import java.util.List;

public record CartResponse(
        List<CartItemResponse> items,
        long totalQuantity,
        String totalAmount
) {
}