package com.wwzyhy.shopagent.exception;

public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException() { super("支付单不存在"); }
}
