package com.wwzyhy.shopagent.dto;

public record AdminProductDetailResponse(
        Long id,
        String title,
        String brand,
        String price,
        String imageUrl,
        String description,
        Integer status
) {
}