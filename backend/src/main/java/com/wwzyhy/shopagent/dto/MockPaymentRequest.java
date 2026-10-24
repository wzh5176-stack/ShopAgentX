package com.wwzyhy.shopagent.dto;

import jakarta.validation.constraints.NotNull;

public record MockPaymentRequest(
        @NotNull(message = "请选择模拟支付结果") MockPaymentOutcome outcome
) {
}
