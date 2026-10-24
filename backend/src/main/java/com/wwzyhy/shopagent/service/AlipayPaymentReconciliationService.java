package com.wwzyhy.shopagent.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wwzyhy.shopagent.entity.PaymentAttempt;
import com.wwzyhy.shopagent.mapper.PaymentAttemptMapper;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "shopagent.alipay.sandbox.enabled", havingValue = "true")
public class AlipayPaymentReconciliationService {
    private static final Logger log = LoggerFactory.getLogger(AlipayPaymentReconciliationService.class);
    private final PaymentAttemptMapper mapper;
    private final AlipayPaymentLookupService lookup;
    private final AlipayTradeQueryService query;
    private final AlipayPaymentSettlementService settlement;
    private final int batchSize;
    private final AtomicLong cursor = new AtomicLong();

    public AlipayPaymentReconciliationService(PaymentAttemptMapper mapper, AlipayPaymentLookupService lookup,
            AlipayTradeQueryService query, AlipayPaymentSettlementService settlement,
            @Value("${shopagent.alipay.sandbox.reconcile.batch-size:20}") int batchSize) {
        if (batchSize < 1 || batchSize > 100) { throw new IllegalArgumentException("查单批量应为1至100"); }
        this.mapper = mapper; this.lookup = lookup; this.query = query;
        this.settlement = settlement; this.batchSize = batchSize;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public int reconcilePending() {
        List<PaymentAttempt> batch = select(cursor.get());
        if (batch.isEmpty()) { cursor.set(0); batch = select(0); }
        int finished = 0;
        for (PaymentAttempt candidate : batch) {
            cursor.set(candidate.getId());
            try {
                PaymentAttempt payment = lookup.pending(candidate.getId());
                if (payment == null) { continue; }
                var result = query.query(payment);
                if (!"PENDING".equals(settlement.reconcile(payment.getId(), result).status().name())) {
                    finished++;
                } else if (result.tradeStatus() == com.wwzyhy.shopagent.dto.AlipayQueryResult.TradeStatus.TRADE_CLOSED) {
                    log.warn("官方交易关闭结果待核对，paymentId={}", payment.getId());
                }
            } catch (RuntimeException exception) {
                // Do not log raw SDK errors, response bodies or buyer information.
                log.warn("官方查单暂未完成，paymentId={}，reason={}", candidate.getId(), exception.getClass().getSimpleName());
            }
        }
        return finished;
    }

    private List<PaymentAttempt> select(long after) {
        QueryWrapper<PaymentAttempt> q = new QueryWrapper<>();
        q.eq("channel", "ALIPAY_SANDBOX").eq("status", "PENDING").gt("id", after)
                .orderByAsc("id").last("LIMIT " + batchSize);
        return mapper.selectList(q);
    }
}
