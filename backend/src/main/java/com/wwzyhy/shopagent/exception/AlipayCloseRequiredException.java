package com.wwzyhy.shopagent.exception;

public class AlipayCloseRequiredException extends OrderConflictException {
    public AlipayCloseRequiredException() {
        super("官方交易尚待确认关闭，当前保留库存");
    }
}
