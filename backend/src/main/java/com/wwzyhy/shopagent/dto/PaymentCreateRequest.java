package com.wwzyhy.shopagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record PaymentCreateRequest(
        @NotNull(message = "订单编号不能为空")
        @Positive(message = "订单编号必须为正整数")
        Long orderId,

        @NotBlank(message = "支付请求编号不能为空")
        @Pattern(
                regexp = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}",
                message = "支付请求编号必须是标准小写UUID")
        String requestKey
) {
}