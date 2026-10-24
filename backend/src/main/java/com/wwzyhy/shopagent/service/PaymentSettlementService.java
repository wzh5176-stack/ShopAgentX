package com.wwzyhy.shopagent.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.wwzyhy.shopagent.dto.MockPaymentOutcome;
import com.wwzyhy.shopagent.dto.OrderStatus;
import com.wwzyhy.shopagent.dto.PaymentGatewayResult;
import com.wwzyhy.shopagent.dto.PaymentResponse;
import com.wwzyhy.shopagent.dto.PaymentStatus;
import com.wwzyhy.shopagent.entity.OrderItem;
import com.wwzyhy.shopagent.entity.PaymentAttempt;
import com.wwzyhy.shopagent.entity.ShopOrder;
import com.wwzyhy.shopagent.exception.PaymentNotFoundException;
import com.wwzyhy.shopagent.mapper.OrderItemMapper;
import com.wwzyhy.shopagent.mapper.PaymentAttemptMapper;
import com.wwzyhy.shopagent.mapper.ProductInventoryMapper;
import com.wwzyhy.shopagent.mapper.ShopOrderMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;import com.wwzyhy.shopagent.exception.PaymentConflictException;

@Service
public class PaymentSettlementService {
    private final CurrentUserService currentUserService;
    private final PaymentAttemptMapper paymentMapper;
    private final ShopOrderMapper orderMapper;
    private final OrderItemMapper itemMapper;
    private final ProductInventoryMapper inventoryMapper;
    private final OrderCloseService closeService;
    private final Clock clock;

    public PaymentSettlementService(CurrentUserService currentUserService,
            PaymentAttemptMapper paymentMapper, ShopOrderMapper orderMapper,
            OrderItemMapper itemMapper, ProductInventoryMapper inventoryMapper,
            OrderCloseService closeService, Clock clock) {
        this.currentUserService = currentUserService;
        this.paymentMapper = paymentMapper;
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
        this.inventoryMapper = inventoryMapper;
        this.closeService = closeService;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentResponse apply(Authentication authentication, Long id, PaymentGatewayResult result) {
        return settle(currentUserService.requireUserId(authentication), id, result);
    }

    // Internal worker only. HTTP callers always use apply with authenticated ownership.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentResponse reconcile(Long id, PaymentGatewayResult result) {
        return settle(null, id, result);
    }

    private PaymentResponse settle(Long userId, Long id, PaymentGatewayResult result) {
        PaymentReadService.validateId(id);
        PaymentAttempt hint = paymentMapper.selectById(id);
        if (hint == null) { throw new PaymentNotFoundException(); }
        QueryWrapper<ShopOrder> oq = new QueryWrapper<>();
        oq.eq("id", hint.getOrderId());
        if (userId != null) { oq.eq("user_id", userId); }
        oq.last("FOR UPDATE");
        ShopOrder order = orderMapper.selectOne(oq);
        if (order == null) { throw new PaymentNotFoundException(); }
        PaymentAttempt payment = lockPayment(id, order.getId());
        if (!"MOCK".equals(payment.getChannel())) {
            throw new PaymentConflictException(
                    "官方沙箱结算正在接线，请稍后重试");
        }
        if (result != null && (!id.equals(result.paymentId())
                || !order.getId().equals(result.orderId())
                || !payment.getChannel().equals(result.channel())
                || result.amount() == null || payment.getAmount().compareTo(result.amount()) != 0
                || order.getTotalAmount().compareTo(result.amount()) != 0
                || result.outcome() == null)) {
            throw new IllegalStateException("渠道结果与支付单不一致");
        }
        if (!PaymentStatus.PENDING.name().equals(payment.getStatus())) {
            return PaymentResponse.from(payment);
        }
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
        if (OrderStatus.PENDING_PAYMENT.name().equals(order.getStatus())
                && !now.isBefore(order.getExpiresAt())) {
            closeService.expireInCurrentTransaction(order.getId());
            return PaymentResponse.from(lockPayment(id, order.getId()));
        }
        if (OrderStatus.CANCELLED.name().equals(order.getStatus())
                || OrderStatus.EXPIRED.name().equals(order.getStatus())
                || !now.isBefore(payment.getExpiresAt())) {
            finish(payment, PaymentStatus.CLOSED, now);
            return PaymentResponse.from(lockPayment(id, order.getId()));
        }
        if (!OrderStatus.PENDING_PAYMENT.name().equals(order.getStatus())) {
            throw new IllegalStateException("订单与支付单状态不一致");
        }
        if (result == null) { return PaymentResponse.from(payment); }

        if (result.outcome() == MockPaymentOutcome.SUCCEEDED) {
            QueryWrapper<OrderItem> iq = new QueryWrapper<>();
            iq.eq("order_id", order.getId()).orderByAsc("product_id").last("FOR SHARE");
            List<OrderItem> items = itemMapper.selectList(iq);
            if (items.isEmpty()) { throw new IllegalStateException("订单明细缺失"); }
            for (OrderItem item : items) {
                if (inventoryMapper.consume(item.getProductId(), item.getQuantity()) != 1) {
                    throw new IllegalStateException("支付库存预占不一致");
                }
            }
            UpdateWrapper<ShopOrder> update = new UpdateWrapper<>();
            update.eq("id", order.getId()).eq("status", OrderStatus.PENDING_PAYMENT.name());
            update.set("status", OrderStatus.PAID.name()).set("paid_at", now).set("updated_at", now);
            if (orderMapper.update(null, update) != 1) { throw new IllegalStateException("更新已支付订单失败"); }
            finish(payment, PaymentStatus.SUCCEEDED, now);
        } else {
            finish(payment, PaymentStatus.FAILED, now);
        }
        return PaymentResponse.from(lockPayment(id, order.getId()));
    }

    private PaymentAttempt lockPayment(Long id, Long orderId) {
        QueryWrapper<PaymentAttempt> q = new QueryWrapper<>();
        q.eq("id", id).eq("order_id", orderId).last("FOR UPDATE");
        PaymentAttempt payment = paymentMapper.selectOne(q);
        if (payment == null) { throw new PaymentNotFoundException(); }
        return payment;
    }

    private void finish(PaymentAttempt payment, PaymentStatus status, LocalDateTime now) {
        UpdateWrapper<PaymentAttempt> update = new UpdateWrapper<>();
        update.eq("id", payment.getId()).eq("status", PaymentStatus.PENDING.name());
        update.set("status", status.name()).set("finished_at", now).set("updated_at", now);
        if (paymentMapper.update(null, update) != 1) { throw new IllegalStateException("更新支付单失败"); }
    }
}
