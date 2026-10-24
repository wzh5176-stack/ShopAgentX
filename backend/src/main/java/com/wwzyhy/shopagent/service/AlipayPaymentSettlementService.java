package com.wwzyhy.shopagent.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.wwzyhy.shopagent.dto.AlipayQueryResult;
import com.wwzyhy.shopagent.dto.AlipayQueryResult.TradeStatus;
import com.wwzyhy.shopagent.dto.OrderStatus;
import com.wwzyhy.shopagent.dto.PaymentResponse;
import com.wwzyhy.shopagent.dto.PaymentStatus;
import com.wwzyhy.shopagent.entity.OrderItem;
import com.wwzyhy.shopagent.entity.PaymentAttempt;
import com.wwzyhy.shopagent.entity.ShopOrder;
import com.wwzyhy.shopagent.exception.PaymentConflictException;
import com.wwzyhy.shopagent.exception.PaymentNotFoundException;
import com.wwzyhy.shopagent.mapper.OrderItemMapper;
import com.wwzyhy.shopagent.mapper.PaymentAttemptMapper;
import com.wwzyhy.shopagent.mapper.ProductInventoryMapper;
import com.wwzyhy.shopagent.mapper.ShopOrderMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "shopagent.alipay.sandbox.enabled", havingValue = "true")
public class AlipayPaymentSettlementService {
    private final CurrentUserService currentUserService;
    private final PaymentAttemptMapper paymentMapper;
    private final ShopOrderMapper orderMapper;
    private final OrderItemMapper itemMapper;
    private final ProductInventoryMapper inventoryMapper;
    private final Clock clock;

    public AlipayPaymentSettlementService(CurrentUserService currentUserService,
            PaymentAttemptMapper paymentMapper, ShopOrderMapper orderMapper,
            OrderItemMapper itemMapper, ProductInventoryMapper inventoryMapper, Clock clock) {
        this.currentUserService = currentUserService;
        this.paymentMapper = paymentMapper;
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
        this.inventoryMapper = inventoryMapper;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentResponse apply(Authentication authentication, Long id, AlipayQueryResult result) {
        return settle(currentUserService.requireUserId(authentication), id, result);
    }

    // Internal expiry reconciliation; HTTP callers always use the owned apply method.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentResponse reconcile(Long id, AlipayQueryResult result) {
        return settle(null, id, result);
    }

    private PaymentResponse settle(Long userId, Long id, AlipayQueryResult result) {
        PaymentReadService.validateId(id);
        PaymentAttempt hint = paymentMapper.selectById(id);
        if (hint == null) {
            throw new PaymentNotFoundException();
        }
        QueryWrapper<ShopOrder> oq = new QueryWrapper<>();
        oq.eq("id", hint.getOrderId());
        if (userId != null) { oq.eq("user_id", userId); }
        oq.last("FOR UPDATE");
        ShopOrder order = orderMapper.selectOne(oq);
        if (order == null) {
            throw new PaymentNotFoundException();
        }
        PaymentAttempt payment = lockPayment(id, order.getId());
        validateResult(payment, order, result);

        if (PaymentStatus.SUCCEEDED.name().equals(payment.getStatus())) {
            if (!OrderStatus.PAID.name().equals(order.getStatus())
                    || (result.tradeStatus().isPaid()
                    && !Objects.equals(payment.getProviderTradeNo(), result.providerTradeNo()))) {
                throw new PaymentConflictException("官方交易与已入账支付不一致，需核对");
            }
            return PaymentResponse.from(payment);
        }
        if (!PaymentStatus.PENDING.name().equals(payment.getStatus())) {
            if (result.tradeStatus().isPaid()) {
                throw new PaymentConflictException("已结束的本地支付出现官方成功结果，需核对");
            }
            return PaymentResponse.from(payment);
        }
        if (!OrderStatus.PENDING_PAYMENT.name().equals(order.getStatus())) {
            throw new PaymentConflictException("订单状态与待处理支付不一致，需核对");
        }
        if (!result.tradeStatus().isPaid()) {
            // NOT_FOUND/WAIT keep reservations. CLOSED may mean refund, so do not infer release.
            return PaymentResponse.from(payment);
        }

        // A delayed confirmed success must not be discarded solely due to our local deadline.
        QueryWrapper<OrderItem> iq = new QueryWrapper<>();
        iq.eq("order_id", order.getId()).orderByAsc("product_id").last("FOR SHARE");
        List<OrderItem> items = itemMapper.selectList(iq);
        if (items.isEmpty()) {
            throw new IllegalStateException("订单明细缺失");
        }
        for (OrderItem item : items) {
            if (inventoryMapper.consume(item.getProductId(), item.getQuantity()) != 1) {
                throw new IllegalStateException("官方支付库存预占不一致");
            }
        }
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
        UpdateWrapper<ShopOrder> ou = new UpdateWrapper<>();
        ou.eq("id", order.getId()).eq("status", OrderStatus.PENDING_PAYMENT.name());
        // Local booking time; the exact provider time is retained separately on the payment.
        ou.set("status", OrderStatus.PAID.name()).set("paid_at", now).set("updated_at", now);
        if (orderMapper.update(null, ou) != 1) {
            throw new IllegalStateException("更新官方已支付订单失败");
        }
        UpdateWrapper<PaymentAttempt> pu = new UpdateWrapper<>();
        pu.eq("id", id).eq("status", PaymentStatus.PENDING.name());
        pu.set("status", PaymentStatus.SUCCEEDED.name()).set("finished_at", now)
                .set("updated_at", now).set("provider_trade_no", result.providerTradeNo())
                .set("provider_paid_at", result.providerPaidAt());
        if (paymentMapper.update(null, pu) != 1) {
            throw new IllegalStateException("更新官方成功支付单失败");
        }
        return PaymentResponse.from(lockPayment(id, order.getId()));
    }

    private void validateResult(PaymentAttempt payment, ShopOrder order, AlipayQueryResult result) {
        if (!"ALIPAY_SANDBOX".equals(payment.getChannel()) || result == null
                || !Objects.equals(payment.getId(), result.paymentId())
                || !Objects.equals(order.getId(), result.orderId())
                || !Objects.equals(payment.getOutTradeNo(), result.outTradeNo())
                || !Objects.equals(payment.getProviderAppId(), result.appId())
                || !Objects.equals(payment.getProviderSellerId(), result.sellerId())
                || result.tradeStatus() == null
                || payment.getAmount().compareTo(order.getTotalAmount()) != 0) {
            throw new PaymentConflictException("官方查询结果与本地订单或身份不一致");
        }
        if (result.tradeStatus() != TradeStatus.NOT_FOUND
                && (result.amount() == null || payment.getAmount().compareTo(result.amount()) != 0
                || result.providerTradeNo() == null
                || !result.providerTradeNo().matches("[A-Za-z0-9_]{1,64}"))) {
            throw new PaymentConflictException("官方交易号或金额不一致");
        }
        if (result.tradeStatus().isPaid() && result.providerPaidAt() == null) {
            throw new PaymentConflictException("官方成功结果缺少付款时间");
        }
        if (result.tradeStatus() != TradeStatus.NOT_FOUND && payment.getProviderTradeNo() != null
                && !payment.getProviderTradeNo().equals(result.providerTradeNo())) {
            throw new PaymentConflictException("官方交易号与已保存结果不一致");
        }
    }

    private PaymentAttempt lockPayment(Long id, Long orderId) {
        QueryWrapper<PaymentAttempt> q = new QueryWrapper<>();
        q.eq("id", id).eq("order_id", orderId).last("FOR UPDATE");
        PaymentAttempt payment = paymentMapper.selectOne(q);
        if (payment == null) {
            throw new PaymentNotFoundException();
        }
        return payment;
    }
}
