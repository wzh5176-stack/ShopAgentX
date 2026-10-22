package com.wwzyhy.shopagent.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartItemSetRequest(
        @NotNull(message = "商品数量不能为空")
        @Min(value = 1, message = "商品数量至少为1")
        @Max(value = 99, message = "商品数量不能超过99")
        Integer quantity
) {
}