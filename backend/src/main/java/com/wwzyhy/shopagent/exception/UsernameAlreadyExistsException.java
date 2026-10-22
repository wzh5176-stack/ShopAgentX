package com.wwzyhy.shopagent.exception;

public class UsernameAlreadyExistsException extends RuntimeException {

    public UsernameAlreadyExistsException() {
        super("用户名已被使用");
    }
}