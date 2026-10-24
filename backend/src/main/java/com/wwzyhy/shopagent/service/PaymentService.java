package com.wwzyhy.shopagent.service;

import com.wwzyhy.shopagent.dto.MockPaymentOutcome;
import com.wwzyhy.shopagent.dto.PaymentResponse;
import com.wwzyhy.shopagent.entity.PaymentAttempt;
import com.wwzyhy.shopagent.exception.AlipayQueryUnavailableException;
import com.wwzyhy.shopagent.exception.PaymentConflictException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {
    private final PaymentReadService readService;
    private final PaymentGateway gateway;
    private final MockPaymentGateway mockGateway;
    private final PaymentSettlementService settlement;
    private final ObjectProvider<AlipayPaymentRefreshService> alipayRefresh;

    public PaymentService(PaymentReadService readService, PaymentGateway gateway,
            MockPaymentGateway mockGateway, PaymentSettlementService settlement,
            ObjectProvider<AlipayPaymentRefreshService> alipayRefresh) {
        this.readService = readService;
        this.gateway = gateway;
        this.mockGateway = mockGateway;
        this.settlement = settlement;
        this.alipayRefresh = alipayRefresh;
    }

    public PaymentResponse get(Authentication authentication, Long id) {
        return PaymentResponse.from(readService.requireOwned(authentication, id));
    }

    public PaymentResponse refresh(
            Authentication authentication,
            Long id) {

        PaymentAttempt payment =
                readService.requireOwned(authentication, id);

        if ("ALIPAY_SANDBOX".equals(payment.getChannel())) {
            if (!"PENDING".equals(payment.getStatus())) {
                return PaymentResponse.from(payment);
            }
            AlipayPaymentRefreshService service = alipayRefresh.getIfAvailable();
            if (service == null) {
                throw new AlipayQueryUnavailableException();
            }
            return service.refresh(authentication, id);
        }
        if (!"MOCK".equals(payment.getChannel())) {
            throw new PaymentConflictException("不支持的支付渠道");
        }

        return settlement.apply(
                authentication,
                id,
                gateway.query(payment));
    }

    public PaymentResponse simulate(Authentication authentication, Long id, MockPaymentOutcome outcome) {
        mockGateway.record(authentication, id, outcome);
        return refresh(authentication, id);
    }
}
