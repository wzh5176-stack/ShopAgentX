package com.wwzyhy.shopagent.exception;

public class AlipayCloseUnavailableException extends RuntimeException {
    public AlipayCloseUnavailableException() {
        super("支付宝沙箱关单暂不可用，库存保持预占，请稍后重试");
    }
}
