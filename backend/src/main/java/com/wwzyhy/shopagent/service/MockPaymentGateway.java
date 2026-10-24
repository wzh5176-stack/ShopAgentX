package com.wwzyhy.shopagent.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wwzyhy.shopagent.dto.MockPaymentOutcome;
import com.wwzyhy.shopagent.dto.OrderStatus;
import com.wwzyhy.shopagent.dto.PaymentGatewayResult;
import com.wwzyhy.shopagent.dto.PaymentStatus;
import com.wwzyhy.shopagent.entity.MockPaymentReceipt;
import com.wwzyhy.shopagent.entity.PaymentAttempt;
import com.wwzyhy.shopagent.entity.ShopOrder;
import com.wwzyhy.shopagent.exception.PaymentConflictException;
import com.wwzyhy.shopagent.exception.PaymentNotFoundException;
import com.wwzyhy.shopagent.exception.PaymentUnavailableException;
import com.wwzyhy.shopagent.mapper.MockPaymentReceiptMapper;
import com.wwzyhy.shopagent.mapper.PaymentAttemptMapper;
import com.wwzyhy.shopagent.mapper.ShopOrderMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MockPaymentGateway implements PaymentGateway {
    private final CurrentUserService currentUserService;
    private final ShopOrderMapper orderMapper;
    private final PaymentAttemptMapper paymentMapper;
    private final MockPaymentReceiptMapper receiptMapper;
    private final Clock clock;
    private final boolean enabled;

    public MockPaymentGateway(CurrentUserService currentUserService, ShopOrderMapper orderMapper,
            PaymentAttemptMapper paymentMapper, MockPaymentReceiptMapper receiptMapper, Clock clock,
            @Value("${shopagent.payments.mock.enabled:false}") boolean enabled) {
        this.currentUserService = currentUserService;
        this.orderMapper = orderMapper;
        this.paymentMapper = paymentMapper;
        this.receiptMapper = receiptMapper;
        this.clock = clock;
        this.enabled = enabled;
    }

    @Override
    public String channel() { return "MOCK"; }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public PaymentGatewayResult query(PaymentAttempt payment) {
        if (!channel().equals(payment.getChannel())) {
            throw new IllegalStateException("不支持的支付渠道");
        }
        MockPaymentReceipt receipt = receiptMapper.selectById(payment.getId());
        return receipt == null ? null : new PaymentGatewayResult(receipt.getPaymentId(),
                receipt.getOrderId(), channel(), receipt.getAmount(),
                MockPaymentOutcome.valueOf(receipt.getOutcome()));
    }

    // Separate committed receipt simulates a durable provider result, without real funds.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Authentication authentication, Long id, MockPaymentOutcome outcome) {
        if (!enabled) { throw new PaymentUnavailableException(); }
        PaymentReadService.validateId(id);
        if (outcome == null) { throw new IllegalArgumentException("请选择模拟结果"); }
        Long userId = currentUserService.requireUserId(authentication);
        PaymentAttempt hint = paymentMapper.selectById(id);
        if (hint == null) { throw new PaymentNotFoundException(); }
        QueryWrapper<ShopOrder> oq = new QueryWrapper<>();
        oq.eq("id", hint.getOrderId()).eq("user_id", userId).last("FOR UPDATE");
        ShopOrder order = orderMapper.selectOne(oq);
        if (order == null) { throw new PaymentNotFoundException(); }
        QueryWrapper<PaymentAttempt> pq = new QueryWrapper<>();
        pq.eq("id", id).eq("order_id", order.getId()).last("FOR UPDATE");
        PaymentAttempt payment = paymentMapper.selectOne(pq);
        if (payment == null || !channel().equals(payment.getChannel())) {
            throw new PaymentNotFoundException();
        }
        QueryWrapper<MockPaymentReceipt> rq = new QueryWrapper<>();
        rq.eq("payment_id", id).last("FOR SHARE");
        MockPaymentReceipt existing = receiptMapper.selectOne(rq);
        if (existing != null) {
            if (!outcome.name().equals(existing.getOutcome())) {
                throw new PaymentConflictException("本次模拟结果已确定，不能改写");
            }
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
        if (!PaymentStatus.PENDING.name().equals(payment.getStatus())
                || !OrderStatus.PENDING_PAYMENT.name().equals(order.getStatus())
                || !now.isBefore(payment.getExpiresAt()) || !now.isBefore(order.getExpiresAt())) {
            throw new PaymentConflictException("支付单已结束或订单已超时，请刷新状态");
        }
        MockPaymentReceipt receipt = new MockPaymentReceipt();
        receipt.setPaymentId(id);
        receipt.setOrderId(order.getId());
        receipt.setAmount(payment.getAmount());
        receipt.setOutcome(outcome.name());
        receipt.setCreatedAt(now);
        if (receiptMapper.insert(receipt) != 1) { throw new IllegalStateException("记录模拟结果失败"); }
    }
}
