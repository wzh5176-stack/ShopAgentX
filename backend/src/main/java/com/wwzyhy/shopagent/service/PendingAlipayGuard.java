package com.wwzyhy.shopagent.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wwzyhy.shopagent.entity.PaymentAttempt;
import com.wwzyhy.shopagent.exception.AlipayCloseRequiredException;
import com.wwzyhy.shopagent.mapper.PaymentAttemptMapper;

public final class PendingAlipayGuard {

    private PendingAlipayGuard() {
    }

    public static void requireLocalCloseAllowed(
            PaymentAttemptMapper mapper,
            Long orderId) {

        QueryWrapper<PaymentAttempt> query =
                new QueryWrapper<>();

        query.eq("order_id", orderId)
                .eq("channel", "ALIPAY_SANDBOX")
                .eq("status", "PENDING")
                .last("FOR UPDATE");

        if (!mapper.selectList(query).isEmpty()) {
            throw new AlipayCloseRequiredException();
        }
    }
}
