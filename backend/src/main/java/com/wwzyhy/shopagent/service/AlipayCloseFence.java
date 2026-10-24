package com.wwzyhy.shopagent.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wwzyhy.shopagent.entity.PaymentAttempt;
import com.wwzyhy.shopagent.exception.PaymentConflictException;
import com.wwzyhy.shopagent.mapper.PaymentAttemptMapper;

public final class AlipayCloseFence {
    private AlipayCloseFence() { }

    // Called while the owning order is locked. A confirmed remote close is durable,
    // even if local inventory release needs a retry; never create a new payment in that gap.
    public static void requirePaymentCreationAllowed(PaymentAttemptMapper mapper, Long orderId) {
        QueryWrapper<PaymentAttempt> q = new QueryWrapper<>();
        q.eq("order_id", orderId).eq("channel", "ALIPAY_SANDBOX")
                .eq("status", "CLOSED").last("FOR SHARE");
        if (!mapper.selectList(q).isEmpty()) {
            throw new PaymentConflictException("官方支付已关闭，请先完成订单关闭，不能重新创建支付");
        }
    }
}
