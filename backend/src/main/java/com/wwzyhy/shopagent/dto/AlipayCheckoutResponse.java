package com.wwzyhy.shopagent.dto;

public record AlipayCheckoutResponse(
        PaymentResponse payment,
        String outTradeNo,
        String payUrl
) {
}