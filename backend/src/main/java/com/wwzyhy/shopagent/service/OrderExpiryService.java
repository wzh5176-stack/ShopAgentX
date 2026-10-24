package com.wwzyhy.shopagent.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wwzyhy.shopagent.dto.OrderStatus;
import com.wwzyhy.shopagent.entity.ShopOrder;
import com.wwzyhy.shopagent.mapper.ShopOrderMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OrderExpiryService {

    private static final Logger log = LoggerFactory.getLogger(OrderExpiryService.class);
    private static final int BATCH_SIZE = 100;

    private final ShopOrderMapper orderMapper;
    private final OrderCloseCoordinator closeService;
    private final Clock clock;

    public OrderExpiryService(
            ShopOrderMapper orderMapper,
            OrderCloseCoordinator closeService,
            Clock clock) {
        this.orderMapper = orderMapper;
        this.closeService = closeService;
        this.clock = clock;
    }

    // Each order closes in its own transaction in another Spring bean.
    public int expireDueOrders() {
        QueryWrapper<ShopOrder> query = new QueryWrapper<>();
        query.select("id");
        query.eq("status", OrderStatus.PENDING_PAYMENT.name());
        query.le("expires_at", LocalDateTime.now(clock));
        query.orderByAsc("expires_at", "id");
        query.last("LIMIT " + BATCH_SIZE);

        int expired = 0;
        for (ShopOrder order : orderMapper.selectList(query)) {
            try {
                if (closeService.expire(order.getId())) {
                    expired++;
                }
            } catch (RuntimeException exception) {
                log.error("关闭超时订单失败，orderId={}", order.getId(), exception);
            }
        }
        return expired;
    }
}
