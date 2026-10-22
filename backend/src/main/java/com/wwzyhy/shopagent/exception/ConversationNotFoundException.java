package com.wwzyhy.shopagent.exception;

public class ConversationNotFoundException extends RuntimeException {

    public ConversationNotFoundException() {
        super("会话不存在");
    }
}