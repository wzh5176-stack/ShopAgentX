package com.wwzyhy.shopagent.dto;

import com.wwzyhy.shopagent.entity.OrderItem;
import com.wwzyhy.shopagent.entity.ShopOrder;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public record OrderDetailResponse(
        Long id,
        OrderStatus status,
        String totalAmount,
        OrderAddressResponse address,
        List<OrderItemResponse> items,
        LocalDateTime expiresAt,
        LocalDateTime paidAt,
        LocalDateTime closedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public OrderDetailResponse {
        items = List.copyOf(items);
    }

    public static OrderDetailResponse from(
            ShopOrder order,
            List<OrderItem> rows) {
        List<OrderItemResponse> items = new ArrayList<>();

        for (OrderItem row : rows) {
            items.add(OrderItemResponse.from(row));
        }

        return new OrderDetailResponse(
                order.getId(),
                OrderStatus.valueOf(order.getStatus()),
                order.getTotalAmount()
                        .setScale(2, RoundingMode.UNNECESSARY)
                        .toPlainString(),
                OrderAddressResponse.from(order),
                items,
                order.getExpiresAt(),
                order.getPaidAt(),
                order.getClosedAt(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }
}