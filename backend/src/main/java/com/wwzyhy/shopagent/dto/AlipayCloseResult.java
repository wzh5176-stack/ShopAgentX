package com.wwzyhy.shopagent.dto;

import java.math.BigDecimal;

// Internal proof; no HTTP endpoint accepts this object from a browser.
public record AlipayCloseResult(Long paymentId, Long orderId, String outTradeNo,
        String appId, String sellerId, BigDecimal amount, Proof proof, String providerTradeNo) {
    public enum Proof { API_CLOSED, EXPIRED_NOT_FOUND }
}
