package com.wwzyhy.shopagent.service;

import com.wwzyhy.shopagent.dto.AlipayCheckoutResponse;
import com.wwzyhy.shopagent.dto.PaymentCreateRequest;
import com.wwzyhy.shopagent.dto.PaymentResponse;
import com.wwzyhy.shopagent.entity.PaymentAttempt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(
        name = "shopagent.alipay.sandbox.enabled",
        havingValue = "true")
public class AlipayCheckoutService {

    private final AlipayPaymentPrepareService prepareService;
    private final AlipayPagePayService pagePayService;

    public AlipayCheckoutService(
            AlipayPaymentPrepareService prepareService,
            AlipayPagePayService pagePayService) {
        this.prepareService = prepareService;
        this.pagePayService = pagePayService;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AlipayCheckoutResponse checkout(
            Authentication authentication,
            PaymentCreateRequest request) {

        PaymentAttempt payment =
                prepareService.prepare(authentication, request);

        String url = pagePayService.createUrl(
                payment.getOutTradeNo(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getExpiresAt());

        return new AlipayCheckoutResponse(
                PaymentResponse.from(payment),
                payment.getOutTradeNo(),
                url);
    }
}