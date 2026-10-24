package com.wwzyhy.shopagent.dto;

public record OrderCreateResult(
        boolean created,
        OrderDetailResponse data
) {
}