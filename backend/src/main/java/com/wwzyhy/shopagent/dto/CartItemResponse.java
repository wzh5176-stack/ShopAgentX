package com.wwzyhy.shopagent.dto;

public record CartItemResponse(
        Long productId,
        String title,
        String brand,
        String price,
        String imageUrl,
        Integer quantity,
        boolean available,
        String subtotal
) {
}