package com.wwzyhy.shopagent.service;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradeCloseModel;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.wwzyhy.shopagent.dto.AlipayCloseResult;
import com.wwzyhy.shopagent.dto.AlipayCloseResult.Proof;
import com.wwzyhy.shopagent.dto.AlipayQueryResult;
import com.wwzyhy.shopagent.dto.AlipayQueryResult.TradeStatus;
import com.wwzyhy.shopagent.entity.PaymentAttempt;
import com.wwzyhy.shopagent.exception.AlipayCloseUnavailableException;
import com.wwzyhy.shopagent.exception.PaymentConflictException;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "shopagent.alipay.sandbox.enabled", havingValue = "true")
public class AlipayTradeCloseService {
    private final AlipayClient client;
    private final String appId;
    private final String sellerId;

    public AlipayTradeCloseService(@Qualifier("alipaySandboxClient") AlipayClient client,
            @Value("${shopagent.alipay.sandbox.app-id}") String appId,
            @Value("${shopagent.alipay.sandbox.seller-id}") String sellerId) {
        this.client = client;
        this.appId = appId;
        this.sellerId = sellerId;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AlipayCloseResult close(PaymentAttempt payment, AlipayQueryResult query) {
        if (payment == null || query == null || !"ALIPAY_SANDBOX".equals(payment.getChannel())
                || !appId.equals(payment.getProviderAppId()) || !sellerId.equals(payment.getProviderSellerId())
                || query.tradeStatus() != TradeStatus.WAIT_BUYER_PAY
                || !Objects.equals(payment.getId(), query.paymentId())
                || !Objects.equals(payment.getOrderId(), query.orderId())
                || !Objects.equals(payment.getOutTradeNo(), query.outTradeNo())
                || !appId.equals(query.appId()) || !sellerId.equals(query.sellerId())
                || query.amount() == null || payment.getAmount().compareTo(query.amount()) != 0) {
            throw new PaymentConflictException("关单查询结果或应用商家身份不一致");
        }
        AlipayTradeCloseModel model = new AlipayTradeCloseModel();
        model.setOutTradeNo(payment.getOutTradeNo());
        AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
        request.setBizModel(model);
        try {
            var response = client.execute(request);
            if (response != null && response.isSuccess()) {
                if (!payment.getOutTradeNo().equals(response.getOutTradeNo())
                        || query.providerTradeNo() == null
                        || !query.providerTradeNo().equals(response.getTradeNo())) {
                    throw new AlipayCloseUnavailableException();
                }
                return new AlipayCloseResult(payment.getId(), payment.getOrderId(),
                        payment.getOutTradeNo(), appId, sellerId, payment.getAmount(),
                        Proof.API_CLOSED, response.getTradeNo());
            }
            if (response != null && "40004".equals(response.getCode())
                    && Set.of("ACQ.TRADE_STATUS_ERROR", "ACQ.TRADE_HAS_SUCCESS", "ACQ.TRADE_NOT_EXIST")
                    .contains(response.getSubCode() == null ? "" : response.getSubCode())) {
                // Could have been paid concurrently. Re-query; this is not close confirmation.
                return null;
            }
            throw new AlipayCloseUnavailableException();
        } catch (AlipayApiException | RuntimeException exception) {
            throw new AlipayCloseUnavailableException();
        }
    }
}
