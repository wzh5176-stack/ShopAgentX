package com.wwzyhy.shopagent.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.wwzyhy.shopagent.dto.OrderDetailResponse;
import com.wwzyhy.shopagent.dto.OrderStatus;
import com.wwzyhy.shopagent.entity.OrderItem;
import com.wwzyhy.shopagent.entity.ShopOrder;
import com.wwzyhy.shopagent.exception.OrderConflictException;
import com.wwzyhy.shopagent.exception.OrderNotFoundException;
import com.wwzyhy.shopagent.mapper.OrderItemMapper;
import com.wwzyhy.shopagent.mapper.ProductInventoryMapper;
import com.wwzyhy.shopagent.mapper.ShopOrderMapper;
import com.wwzyhy.shopagent.mapper.PaymentAttemptMapper;
import com.wwzyhy.shopagent.entity.PaymentAttempt;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderCloseService {

    private final CurrentUserService currentUserService;
    private final ShopOrderMapper orderMapper;
    private final OrderItemMapper itemMapper;
    private final ProductInventoryMapper inventoryMapper;
    private final Clock clock;
    private final PaymentAttemptMapper paymentMapper;

    public OrderCloseService(
            CurrentUserService currentUserService,
            ShopOrderMapper orderMapper,
            OrderItemMapper itemMapper,
            ProductInventoryMapper inventoryMapper,
            Clock clock,
            PaymentAttemptMapper paymentMapper) {
        this.currentUserService = currentUserService;
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
        this.inventoryMapper = inventoryMapper;
        this.clock = clock;
        this.paymentMapper = paymentMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OrderDetailResponse cancel(Authentication authentication, Long id) {
        validateId(id);
        Long userId = currentUserService.requireUserId(authentication);
        ShopOrder order = lockOrder(id, userId);
        if (order == null) {
            throw new OrderNotFoundException();
        }

        OrderStatus status = OrderStatus.valueOf(order.getStatus());
        if (status == OrderStatus.PAID) {
            throw new OrderConflictException("已支付订单不能取消");
        }
        if (status == OrderStatus.PENDING_PAYMENT) {
            LocalDateTime now = now();
            OrderStatus target = now.isBefore(order.getExpiresAt())
                    ? OrderStatus.CANCELLED : OrderStatus.EXPIRED;
            releaseAndClose(order, target, now);
        }

        return readSaved(userId, id);
    }

    // Internal expiry worker only; no public HTTP endpoint accepts this call.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean expire(Long id) {
        return expireLocked(id);
    }

    // Used by payment reconciliation while holding the same order in the same transaction.
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean expireInCurrentTransaction(Long id) {
        return expireLocked(id);
    }

    private boolean expireLocked(Long id) {
        validateId(id);
        ShopOrder order = lockOrder(id, null);
        if (order == null || !OrderStatus.PENDING_PAYMENT.name().equals(order.getStatus())) {
            return false;
        }
        LocalDateTime now = now();
        if (now.isBefore(order.getExpiresAt())) {
            return false;
        }
        releaseAndClose(order, OrderStatus.EXPIRED, now);
        return true;
    }

    private ShopOrder lockOrder(Long id, Long userId) {
        QueryWrapper<ShopOrder> query = new QueryWrapper<>();
        query.eq("id", id);
        if (userId != null) {
            query.eq("user_id", userId);
        }
        query.last("FOR UPDATE");
        return orderMapper.selectOne(query);
    }

    private void releaseAndClose(ShopOrder order, OrderStatus target, LocalDateTime now) {
        PendingAlipayGuard.requireLocalCloseAllowed(
                paymentMapper,
                order.getId());
        UpdateWrapper<PaymentAttempt> closePayments = new UpdateWrapper<>();
        closePayments.eq("order_id", order.getId()).eq("status", "PENDING");
        closePayments.set("status", "CLOSED").set("finished_at", now).set("updated_at", now);
        paymentMapper.update(null, closePayments);

        QueryWrapper<OrderItem> query = new QueryWrapper<>();
        query.eq("order_id", order.getId());
        query.orderByAsc("product_id");
        query.last("FOR SHARE");
        List<OrderItem> items = itemMapper.selectList(query);
        if (items.isEmpty()) {
            throw new IllegalStateException("订单明细缺失，无法释放库存");
        }

        for (OrderItem item : items) {
            if (inventoryMapper.release(item.getProductId(), item.getQuantity()) != 1) {
                throw new IllegalStateException("订单库存预占不一致，无法关闭订单");
            }
        }

        UpdateWrapper<ShopOrder> update = new UpdateWrapper<>();
        update.eq("id", order.getId());
        update.eq("user_id", order.getUserId());
        update.eq("status", OrderStatus.PENDING_PAYMENT.name());
        update.set("status", target.name());
        update.set("closed_at", now);
        update.set("updated_at", now);
        if (orderMapper.update(null, update) != 1) {
            throw new IllegalStateException("关闭订单失败");
        }
    }

    private OrderDetailResponse readSaved(Long userId, Long id) {
        QueryWrapper<ShopOrder> query = new QueryWrapper<>();
        query.eq("id", id);
        query.eq("user_id", userId);
        query.last("FOR SHARE");
        ShopOrder order = orderMapper.selectOne(query);

        QueryWrapper<OrderItem> itemQuery = new QueryWrapper<>();
        itemQuery.eq("order_id", id);
        itemQuery.orderByAsc("id");
        itemQuery.last("FOR SHARE");
        List<OrderItem> items = itemMapper.selectList(itemQuery);
        if (order == null || items.isEmpty()) {
            throw new IllegalStateException("读取关闭后的订单失败");
        }
        return OrderDetailResponse.from(order, items);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("订单编号必须为正整数");
        }
    }
}
