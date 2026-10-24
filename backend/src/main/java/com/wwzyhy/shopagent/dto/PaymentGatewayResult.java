package com.wwzyhy.shopagent.dto;

import java.math.BigDecimal;

// Trusted adapter result, never deserialized from a merchant HTTP request.
public record PaymentGatewayResult(
        Long paymentId, Long orderId, String channel,
        BigDecimal amount, MockPaymentOutcome outcome
) {
}
