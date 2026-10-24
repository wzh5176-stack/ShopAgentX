package com.wwzyhy.shopagent.dto;

import com.wwzyhy.shopagent.entity.PaymentAttempt;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long orderId,
        String channel,
        PaymentStatus status,
        String amount,
        LocalDateTime expiresAt,
        LocalDateTime finishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PaymentResponse from(PaymentAttempt payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getChannel(),
                PaymentStatus.valueOf(payment.getStatus()),
                payment.getAmount()
                        .setScale(2, RoundingMode.UNNECESSARY)
                        .toPlainString(),
                payment.getExpiresAt(),
                payment.getFinishedAt(),
                payment.getCreatedAt(),
                payment.getUpdatedAt());
    }
}