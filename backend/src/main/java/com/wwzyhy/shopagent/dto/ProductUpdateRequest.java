package com.wwzyhy.shopagent.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductUpdateRequest(
        @NotBlank(message = "商品名称不能为空")
        @Size(max = 200, message = "商品名称最多200个字符")
        String title,

        @NotBlank(message = "品牌不能为空")
        @Size(max = 100, message = "品牌最多100个字符")
        String brand,

        @NotNull(message = "价格不能为空")
        @DecimalMin(value = "0.00", message = "价格不能为负数")
        @Digits(integer = 8, fraction = 2,
                message = "价格最多8位整数和2位小数")
        BigDecimal price,

        @Size(max = 500, message = "图片地址最多500个字符")
        String imageUrl,

        @NotNull(message = "商品介绍不能为空值")
        @Size(max = 10000, message = "商品介绍最多10000个字符")
        String description,

        @NotNull(message = "商品状态不能为空")
        @Min(value = 0, message = "商品状态只能是0或1")
        @Max(value = 1, message = "商品状态只能是0或1")
        Integer status
) {
}