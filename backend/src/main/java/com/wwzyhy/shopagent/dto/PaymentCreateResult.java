package com.wwzyhy.shopagent.dto;

public record PaymentCreateResult(
        boolean created,
        PaymentResponse data
) {
}