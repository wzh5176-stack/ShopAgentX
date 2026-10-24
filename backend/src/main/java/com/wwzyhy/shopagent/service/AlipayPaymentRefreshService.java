package com.wwzyhy.shopagent.service;

import com.wwzyhy.shopagent.dto.PaymentResponse;
import com.wwzyhy.shopagent.entity.PaymentAttempt;
import com.wwzyhy.shopagent.exception.PaymentConflictException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "shopagent.alipay.sandbox.enabled", havingValue = "true")
public class AlipayPaymentRefreshService {
    private final PaymentReadService readService;
    private final AlipayTradeQueryService queryService;
    private final AlipayPaymentSettlementService settlement;

    public AlipayPaymentRefreshService(PaymentReadService readService,
            AlipayTradeQueryService queryService, AlipayPaymentSettlementService settlement) {
        this.readService = readService;
        this.queryService = queryService;
        this.settlement = settlement;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PaymentResponse refresh(Authentication authentication, Long id) {
        PaymentAttempt payment = readService.requireOwned(authentication, id);
        if (!"ALIPAY_SANDBOX".equals(payment.getChannel())) {
            throw new PaymentConflictException("不是支付宝沙箱支付单");
        }
        if (!"PENDING".equals(payment.getStatus())) {
            return PaymentResponse.from(payment);
        }
        // No business row locks or transaction stay open during the remote request.
        var result = queryService.query(payment);
        return settlement.apply(authentication, id, result);
    }
}
