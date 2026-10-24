package com.wwzyhy.shopagent.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record OrderCreateRequest(
        @NotBlank(message = "创建请求编号不能为空")
        @Pattern(
                regexp = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}",
                message = "创建请求编号必须是标准小写UUID")
        String requestKey,

        @NotNull(message = "收货地址不能为空")
        @Positive(message = "地址编号必须为正整数")
        Long addressId,

        @NotEmpty(message = "请选择至少一种商品")
        @Size(max = 20, message = "一次最多购买20种商品")
        List<@NotNull @Valid CheckoutItemRequest> items
) {
}