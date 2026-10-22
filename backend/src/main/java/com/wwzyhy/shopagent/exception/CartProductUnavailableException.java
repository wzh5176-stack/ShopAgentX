package com.wwzyhy.shopagent.exception;

public class CartProductUnavailableException extends RuntimeException {

    public CartProductUnavailableException() {
        super("商品不存在、已下架或已删除，无法设置购物车数量");
    }
}