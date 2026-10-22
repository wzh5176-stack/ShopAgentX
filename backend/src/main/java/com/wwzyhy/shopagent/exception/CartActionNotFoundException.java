package com.wwzyhy.shopagent.exception;

public class CartActionNotFoundException extends RuntimeException {

    public CartActionNotFoundException() {
        super("购物车操作不存在");
    }
}