package com.wwzyhy.shopagent.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wwzyhy.shopagent.dto.OrderDetailResponse;
import com.wwzyhy.shopagent.dto.OrderPageResponse;
import com.wwzyhy.shopagent.dto.OrderStatus;
import com.wwzyhy.shopagent.dto.OrderSummaryResponse;
import com.wwzyhy.shopagent.dto.PreparedOrderRequest;
import com.wwzyhy.shopagent.entity.OrderItem;
import com.wwzyhy.shopagent.entity.ShopOrder;
import com.wwzyhy.shopagent.exception.OrderConflictException;
import com.wwzyhy.shopagent.exception.OrderNotFoundException;
import com.wwzyhy.shopagent.mapper.OrderItemMapper;
import com.wwzyhy.shopagent.mapper.ShopOrderMapper;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderReadService {

    private final CurrentUserService currentUserService;
    private final ShopOrderMapper orderMapper;
    private final OrderItemMapper itemMapper;

    public OrderReadService(
            CurrentUserService currentUserService,
            ShopOrderMapper orderMapper,
            OrderItemMapper itemMapper) {
        this.currentUserService = currentUserService;
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public OrderDetailResponse findByRequest(
            Long userId,
            PreparedOrderRequest request) {
        QueryWrapper<ShopOrder> query = new QueryWrapper<>();
        query.eq("user_id", userId);
        query.eq("request_key", request.requestKey());

        ShopOrder order = orderMapper.selectOne(query);

        if (order == null) {
            return null;
        }

        if (!request.requestHash().equals(order.getRequestHash())) {
            throw new OrderConflictException(
                    "同一请求编号不能用于不同的下单内容");
        }

        return toResponse(order);
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public OrderDetailResponse get(
            Authentication authentication,
            Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("订单编号必须为正整数");
        }

        Long userId = currentUserService.requireUserId(authentication);

        QueryWrapper<ShopOrder> query = new QueryWrapper<>();
        query.eq("id", id);
        query.eq("user_id", userId);

        ShopOrder order = orderMapper.selectOne(query);

        if (order == null) {
            throw new OrderNotFoundException();
        }

        return toResponse(order);
    }

    private OrderDetailResponse toResponse(ShopOrder order) {
        QueryWrapper<OrderItem> query = new QueryWrapper<>();
        query.eq("order_id", order.getId());
        query.orderByAsc("id");

        List<OrderItem> rows = itemMapper.selectList(query);

        if (rows.isEmpty()) {
            throw new IllegalStateException("订单明细缺失");
        }

        return OrderDetailResponse.from(order, rows);
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public OrderPageResponse list(
            Authentication authentication,
            int page,
            int size,
            OrderStatus status) {
        if (page < 1 || size < 1 || size > 50) {
            throw new IllegalArgumentException("分页参数不符合要求");
        }

        Long userId = currentUserService.requireUserId(authentication);
        QueryWrapper<ShopOrder> query = new QueryWrapper<>();
        query.select("id", "status", "total_amount", "expires_at", "created_at");
        query.eq("user_id", userId);
        if (status != null) {
            query.eq("status", status.name());
        }
        query.orderByDesc("id");

        Page<ShopOrder> result = orderMapper.selectPage(new Page<>(page, size), query);
        List<OrderSummaryResponse> items = result.getRecords().stream()
                .map(OrderSummaryResponse::from)
                .toList();

        return new OrderPageResponse(items, result.getCurrent(), result.getSize(),
                result.getTotal(), result.getPages());
    }
}
