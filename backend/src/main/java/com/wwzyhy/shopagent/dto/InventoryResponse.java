package com.wwzyhy.shopagent.dto;

public record InventoryResponse(Long productId, int availableQuantity, int reservedQuantity) {
}
