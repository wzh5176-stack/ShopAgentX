package com.wwzyhy.shopagent.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.wwzyhy.shopagent.dto.AlipayCloseResult;
import com.wwzyhy.shopagent.dto.AlipayCloseResult.Proof;
import com.wwzyhy.shopagent.entity.PaymentAttempt;
import com.wwzyhy.shopagent.entity.ShopOrder;
import com.wwzyhy.shopagent.exception.OrderConflictException;
import com.wwzyhy.shopagent.exception.OrderNotFoundException;
import com.wwzyhy.shopagent.mapper.PaymentAttemptMapper;
import com.wwzyhy.shopagent.mapper.ShopOrderMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "shopagent.alipay.sandbox.enabled", havingValue = "true")
public class AlipayPaymentCloseService {
    // Signed page-pay links retain their fixed absolute deadline; allow clock/processing slack.
    public static final int EXPIRED_LINK_GRACE_MINUTES = 5;
    private final CurrentUserService currentUserService;
    private final ShopOrderMapper orderMapper;
    private final PaymentAttemptMapper paymentMapper;
    private final Clock clock;

    public AlipayPaymentCloseService(CurrentUserService currentUserService,
            ShopOrderMapper orderMapper, PaymentAttemptMapper paymentMapper, Clock clock) {
        this.currentUserService = currentUserService;
        this.orderMapper = orderMapper;
        this.paymentMapper = paymentMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public PaymentAttempt pendingOwned(Authentication authentication, Long orderId) {
        return pending(currentUserService.requireUserId(authentication), orderId, false);
    }

    // Internal expiry worker only; not an HTTP entry point.
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public PaymentAttempt pendingForExpiry(Long orderId) {
        return pending(null, orderId, true);
    }

    private PaymentAttempt pending(Long userId, Long orderId, boolean expiry) {
        validateId(orderId);
        QueryWrapper<ShopOrder> oq = new QueryWrapper<>();
        oq.eq("id", orderId);
        if (userId != null) { oq.eq("user_id", userId); }
        ShopOrder order = orderMapper.selectOne(oq);
        if (order == null) {
            if (expiry) { return null; }
            throw new OrderNotFoundException();
        }
        if (!"PENDING_PAYMENT".equals(order.getStatus())
                || (expiry && LocalDateTime.now(clock).isBefore(order.getExpiresAt()))) {
            return null;
        }
        QueryWrapper<PaymentAttempt> pq = new QueryWrapper<>();
        pq.eq("order_id", orderId).eq("channel", "ALIPAY_SANDBOX").eq("status", "PENDING");
        return paymentMapper.selectOne(pq);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordClosed(Authentication authentication, AlipayCloseResult result) {
        if (authentication == null) { throw new IllegalArgumentException("缺少用户身份"); }
        record(currentUserService.requireUserId(authentication), result);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordClosedForExpiry(AlipayCloseResult result) {
        record(null, result);
    }

    private void record(Long userId, AlipayCloseResult result) {
        if (result == null || result.proof() == null) {
            throw new IllegalArgumentException("缺少关单结果");
        }
        validateId(result.orderId());
        QueryWrapper<ShopOrder> oq = new QueryWrapper<>();
        oq.eq("id", result.orderId());
        if (userId != null) { oq.eq("user_id", userId); }
        oq.last("FOR UPDATE");
        ShopOrder order = orderMapper.selectOne(oq);
        if (order == null) { throw new OrderNotFoundException(); }
        QueryWrapper<PaymentAttempt> pq = new QueryWrapper<>();
        pq.eq("id", result.paymentId()).eq("order_id", order.getId()).last("FOR UPDATE");
        PaymentAttempt payment = paymentMapper.selectOne(pq);
        if (payment == null || !"ALIPAY_SANDBOX".equals(payment.getChannel())
                || !Objects.equals(payment.getOutTradeNo(), result.outTradeNo())
                || !Objects.equals(payment.getProviderAppId(), result.appId())
                || !Objects.equals(payment.getProviderSellerId(), result.sellerId())
                || result.amount() == null || payment.getAmount().compareTo(result.amount()) != 0
                || order.getTotalAmount().compareTo(result.amount()) != 0) {
            throw new OrderConflictException("关单证明与本地支付信息不一致");
        }
        if ("SUCCEEDED".equals(payment.getStatus()) || "PAID".equals(order.getStatus())) {
            throw new OrderConflictException("订单已付款，不能取消或释放库存");
        }
        if ("CLOSED".equals(payment.getStatus())) {
            if (!Objects.equals(payment.getProviderTradeNo(), result.providerTradeNo())) {
                throw new OrderConflictException("已保存关单结果不一致，需核对");
            }
            return;
        }
        if (!"PENDING_PAYMENT".equals(order.getStatus()) || !"PENDING".equals(payment.getStatus())) {
            throw new OrderConflictException("订单或支付状态已改变，请重新查询");
        }
        if (result.proof() == Proof.API_CLOSED) {
            if (result.providerTradeNo() == null || !result.providerTradeNo().matches("[A-Za-z0-9_]{1,64}")) {
                throw new OrderConflictException("关单确认缺少官方交易号");
            }
        } else if (result.providerTradeNo() != null || !expiredLink(payment, clock)) {
            throw new OrderConflictException("付款链接仍可能有效，暂时保留库存");
        }
        UpdateWrapper<PaymentAttempt> update = new UpdateWrapper<>();
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
        update.eq("id", payment.getId()).eq("status", "PENDING");
        update.set("status", "CLOSED").set("finished_at", now).set("updated_at", now)
                .set("provider_trade_no", result.providerTradeNo());
        if (paymentMapper.update(null, update) != 1) {
            throw new IllegalStateException("保存官方关单确认失败");
        }
        // This commits before local order/inventory closure. Retry can resume without another RPC.
    }

    public static boolean expiredLink(PaymentAttempt payment, Clock clock) {
        return !LocalDateTime.now(clock).isBefore(payment.getExpiresAt().plusMinutes(EXPIRED_LINK_GRACE_MINUTES));
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) { throw new IllegalArgumentException("订单编号必须为正整数"); }
    }
}
