package com.wwzyhy.shopagent.service;

import com.wwzyhy.shopagent.dto.AlipayCloseResult;
import com.wwzyhy.shopagent.dto.AlipayCloseResult.Proof;
import com.wwzyhy.shopagent.dto.AlipayQueryResult;
import com.wwzyhy.shopagent.dto.AlipayQueryResult.TradeStatus;
import com.wwzyhy.shopagent.entity.PaymentAttempt;
import com.wwzyhy.shopagent.exception.AlipayCloseUnavailableException;
import com.wwzyhy.shopagent.exception.OrderConflictException;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "shopagent.alipay.sandbox.enabled", havingValue = "true")
public class AlipayOrderCloseService {
    private final AlipayPaymentCloseService paymentClose;
    private final AlipayTradeQueryService query;
    private final AlipayTradeCloseService tradeClose;
    private final AlipayPaymentSettlementService settlement;
    private final Clock clock;

    public AlipayOrderCloseService(AlipayPaymentCloseService paymentClose,
            AlipayTradeQueryService query, AlipayTradeCloseService tradeClose,
            AlipayPaymentSettlementService settlement, Clock clock) {
        this.paymentClose = paymentClose;
        this.query = query;
        this.tradeClose = tradeClose;
        this.settlement = settlement;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void resolveOwned(Authentication authentication, Long orderId) {
        resolve(authentication, paymentClose.pendingOwned(authentication, orderId));
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void resolveForExpiry(Long orderId) {
        resolve(null, paymentClose.pendingForExpiry(orderId));
    }

    private void resolve(Authentication authentication, PaymentAttempt payment) {
        if (payment == null) { return; }
        AlipayQueryResult current = query.query(payment);
        if (current.tradeStatus().isPaid()) {
            settlePaid(authentication, payment, current);
            return;
        }
        AlipayCloseResult proof = null;
        if (current.tradeStatus() == TradeStatus.WAIT_BUYER_PAY) {
            proof = tradeClose.close(payment, current);
            if (proof == null) {
                current = query.query(payment);
                if (current.tradeStatus().isPaid()) {
                    settlePaid(authentication, payment, current);
                    return;
                }
            }
        }
        if (proof == null) {
            if (current.tradeStatus() == TradeStatus.NOT_FOUND) {
                if (!AlipayPaymentCloseService.expiredLink(payment, clock)) {
                    throw new OrderConflictException("交易尚未创建且付款链接仍可能有效，请等链接过期及缓冲后重试");
                }
                proof = new AlipayCloseResult(payment.getId(), payment.getOrderId(),
                        payment.getOutTradeNo(), current.appId(), current.sellerId(), payment.getAmount(),
                        Proof.EXPIRED_NOT_FOUND, null);
            } else if (current.tradeStatus() == TradeStatus.TRADE_CLOSED) {
                // CLOSED may also mean full refund. An unrecorded timeout/closure needs review.
                throw new OrderConflictException("官方交易已关闭但缺少本地关单确认，请核对交易，库存暂保留");
            } else {
                throw new AlipayCloseUnavailableException();
            }
        }
        if (authentication == null) {
            paymentClose.recordClosedForExpiry(proof);
        } else {
            paymentClose.recordClosed(authentication, proof);
        }
    }

    private void settlePaid(Authentication authentication, PaymentAttempt payment, AlipayQueryResult result) {
        if (authentication == null) {
            settlement.reconcile(payment.getId(), result);
        } else {
            settlement.apply(authentication, payment.getId(), result);
            throw new OrderConflictException("支付宝已确认付款，订单已入账，不能取消");
        }
    }
}
