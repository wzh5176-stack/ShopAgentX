package com.wwzyhy.shopagent.dto;

public record ProductDetailResponse(
        Long id,
        String title,
        String brand,
        String price,
        String imageUrl,
        String description
) {
}