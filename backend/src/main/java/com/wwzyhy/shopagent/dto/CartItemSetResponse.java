package com.wwzyhy.shopagent.dto;

public record CartItemSetResponse(
        Long productId,
        Integer quantity
) {
}