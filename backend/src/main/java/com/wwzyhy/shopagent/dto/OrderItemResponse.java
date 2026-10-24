package com.wwzyhy.shopagent.dto;

import com.wwzyhy.shopagent.entity.OrderItem;
import java.math.RoundingMode;

public record OrderItemResponse(
        Long id,
        Long productId,
        String title,
        String brand,
        String imageUrl,
        String unitPrice,
        Integer quantity,
        String lineAmount
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getTitle(),
                item.getBrand(),
                item.getImageUrl(),
                item.getUnitPrice()
                        .setScale(2, RoundingMode.UNNECESSARY)
                        .toPlainString(),
                item.getQuantity(),
                item.getLineAmount()
                        .setScale(2, RoundingMode.UNNECESSARY)
                        .toPlainString());
    }
}