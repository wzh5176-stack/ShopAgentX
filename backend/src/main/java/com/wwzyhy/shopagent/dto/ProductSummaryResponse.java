package com.wwzyhy.shopagent.dto;

public record ProductSummaryResponse(
        Long id,
        String title,
        String brand,
        String price,
        String imageUrl
) {
}