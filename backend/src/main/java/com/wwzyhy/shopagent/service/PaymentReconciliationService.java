package com.wwzyhy.shopagent.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wwzyhy.shopagent.entity.PaymentAttempt;
import com.wwzyhy.shopagent.mapper.PaymentAttemptMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentReconciliationService {
    private static final Logger log = LoggerFactory.getLogger(PaymentReconciliationService.class);
    private final PaymentAttemptMapper mapper;
    private final PaymentGateway gateway;
    private final PaymentSettlementService settlement;

    public PaymentReconciliationService(PaymentAttemptMapper mapper, PaymentGateway gateway,
            PaymentSettlementService settlement) {
        this.mapper = mapper;
        this.gateway = gateway;
        this.settlement = settlement;
    }

    public int reconcilePending() {
        QueryWrapper<PaymentAttempt> q = new QueryWrapper<>();
        q.eq("status", "PENDING")
                .eq("channel", "MOCK")
                .orderByAsc("expires_at", "id")
                .last("LIMIT 100");
        int finished = 0;
        for (PaymentAttempt payment : mapper.selectList(q)) {
            try {
                if (!"PENDING".equals(settlement.reconcile(payment.getId(), gateway.query(payment)).status().name())) {
                    finished++;
                }
            } catch (RuntimeException exception) {
                log.error("支付查单补偿失败，paymentId={}", payment.getId(), exception);
            }
        }
        return finished;
    }
}
