package com.wwzyhy.shopagent.dto;

public record AdminProductSummaryResponse(
        Long id,
        String title,
        String brand,
        String price,
        String imageUrl,
        Integer status
) {
}