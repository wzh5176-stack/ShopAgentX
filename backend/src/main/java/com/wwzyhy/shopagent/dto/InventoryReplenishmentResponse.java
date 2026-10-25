package com.wwzyhy.shopagent.dto;

import java.time.LocalDateTime;

public record InventoryReplenishmentResponse(Long id, Long productId, String requestKey,
        int quantity, String reason, String actor, int availableQuantity, int reservedQuantity,
        LocalDateTime createdAt) {
}
