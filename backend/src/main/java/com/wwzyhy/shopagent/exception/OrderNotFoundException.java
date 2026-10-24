package com.wwzyhy.shopagent.exception;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException() {
        super("订单不存在");
    }
}
