package com.wwzyhy.shopagent.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Internal result of a server-side, SDK-verified query; never accepted from HTTP input.
public record AlipayQueryResult(
        Long paymentId,
        Long orderId,
        String outTradeNo,
        String appId,
        String sellerId,
        TradeStatus tradeStatus,
        BigDecimal amount,
        String providerTradeNo,
        LocalDateTime providerPaidAt) {

    public enum TradeStatus {
        NOT_FOUND, WAIT_BUYER_PAY, TRADE_CLOSED, TRADE_SUCCESS, TRADE_FINISHED;

        public boolean isPaid() {
            return this == TRADE_SUCCESS || this == TRADE_FINISHED;
        }
    }
}
