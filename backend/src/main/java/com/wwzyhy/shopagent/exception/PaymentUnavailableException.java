package com.wwzyhy.shopagent.exception;

public class PaymentUnavailableException extends RuntimeException {
    public PaymentUnavailableException() { super("本地模拟支付未启用"); }
}
