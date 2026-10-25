package com.wwzyhy.shopagent.dto;

public record ProductSummaryResponse(
        Long id,
        String title,
        String brand,
        String price,
        String imageUrl,
        Integer availableQuantity
) {
    // 旧导购消息是历史快照，不把缺少库存字段误当作当前库存0。
    public ProductSummaryResponse(Long id, String title, String brand, String price, String imageUrl) {
        this(id, title, brand, price, imageUrl, null);
    }
}
