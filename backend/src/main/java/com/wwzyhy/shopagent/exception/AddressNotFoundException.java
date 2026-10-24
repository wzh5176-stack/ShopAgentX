package com.wwzyhy.shopagent.exception;

public class AddressNotFoundException extends RuntimeException {

    public AddressNotFoundException() {
        super("地址不存在");
    }
}