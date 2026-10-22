package com.wwzyhy.shopagent.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ProductStatusRequest(
        @NotNull(message = "商品状态不能为空")
        @Min(value = 0, message = "商品状态只能是0或1")
        @Max(value = 1, message = "商品状态只能是0或1")
        Integer status
) {
}