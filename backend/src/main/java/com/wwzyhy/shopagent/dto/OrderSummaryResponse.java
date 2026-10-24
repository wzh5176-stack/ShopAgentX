package com.wwzyhy.shopagent.dto;

import com.wwzyhy.shopagent.entity.ShopOrder;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public record OrderSummaryResponse(
        Long id,
        OrderStatus status,
        String totalAmount,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {
    public static OrderSummaryResponse from(ShopOrder order) {
        return new OrderSummaryResponse(
                order.getId(), OrderStatus.valueOf(order.getStatus()),
                order.getTotalAmount().setScale(2, RoundingMode.UNNECESSARY)
                        .toPlainString(),
                order.getExpiresAt(), order.getCreatedAt());
    }
}
