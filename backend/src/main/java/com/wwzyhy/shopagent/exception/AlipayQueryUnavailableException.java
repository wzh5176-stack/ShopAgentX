package com.wwzyhy.shopagent.exception;

public class AlipayQueryUnavailableException extends RuntimeException {
    public AlipayQueryUnavailableException() {
        // SDK exceptions can contain raw requests; do not expose or log them as a cause.
        super("支付宝沙箱查询暂不可用，支付状态保持不变，请稍后重试");
    }
}
