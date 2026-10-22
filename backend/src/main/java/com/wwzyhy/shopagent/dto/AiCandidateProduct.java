package com.wwzyhy.shopagent.dto;

public record AiCandidateProduct(
        Long id,
        String title,
        String brand,
        String price,
        String description
) {
}