package com.wwzyhy.shopagent.exception;

public class RecommendationSourceNotFoundException extends RuntimeException {

    public RecommendationSourceNotFoundException() {
        super("推荐消息不存在");
    }
}