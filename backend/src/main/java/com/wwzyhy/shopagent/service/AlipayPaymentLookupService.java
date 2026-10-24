package com.wwzyhy.shopagent.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wwzyhy.shopagent.entity.PaymentAttempt;
import com.wwzyhy.shopagent.mapper.PaymentAttemptMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlipayPaymentLookupService {
    private final PaymentAttemptMapper mapper;

    public AlipayPaymentLookupService(PaymentAttemptMapper mapper) { this.mapper = mapper; }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public PaymentAttempt findByTradeNo(String outTradeNo) {
        QueryWrapper<PaymentAttempt> q = new QueryWrapper<>();
        q.eq("channel", "ALIPAY_SANDBOX").eq("out_trade_no", outTradeNo);
        return mapper.selectOne(q);
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public PaymentAttempt pending(Long id) {
        PaymentAttempt payment = mapper.selectById(id);
        return payment != null && "ALIPAY_SANDBOX".equals(payment.getChannel())
                && "PENDING".equals(payment.getStatus()) ? payment : null;
    }
}
