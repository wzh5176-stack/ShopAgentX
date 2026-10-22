package com.wwzyhy.shopagent.exception;

public class ConversationChangedException extends RuntimeException {

    public ConversationChangedException() {
        super("会话已有新消息");
    }
}