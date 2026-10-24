package com.wwzyhy.shopagent.dto;

public record CheckoutItemResponse(
        Long productId,
        String title,
        String brand,
        String imageUrl,
        String unitPrice,
        Integer quantity,
        String lineAmount
) {
}