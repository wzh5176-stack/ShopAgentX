package com.wwzyhy.shopagent.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CartActionCreateRequest(
        @NotNull(message = "来源消息编号不能为空")
        @Positive(message = "来源消息编号必须为正整数")
        Long sourceMessageId,

        @NotNull(message = "商品编号不能为空")
        @Positive(message = "商品编号必须为正整数")
        Long productId,

        @NotNull(message = "目标数量不能为空")
        @Min(value = 1, message = "目标数量至少为1")
        @Max(value = 99, message = "目标数量不能超过99")
        Integer quantity
) {
}