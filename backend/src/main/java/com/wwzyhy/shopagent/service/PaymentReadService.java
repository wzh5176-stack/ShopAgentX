package com.wwzyhy.shopagent.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wwzyhy.shopagent.entity.PaymentAttempt;
import com.wwzyhy.shopagent.entity.ShopOrder;
import com.wwzyhy.shopagent.dto.PaymentResponse;
import com.wwzyhy.shopagent.exception.OrderNotFoundException;
import java.util.List;
import com.wwzyhy.shopagent.exception.PaymentNotFoundException;
import com.wwzyhy.shopagent.mapper.PaymentAttemptMapper;
import com.wwzyhy.shopagent.mapper.ShopOrderMapper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentReadService {
    private final CurrentUserService currentUserService;
    private final PaymentAttemptMapper paymentMapper;
    private final ShopOrderMapper orderMapper;

    public PaymentReadService(CurrentUserService currentUserService,
            PaymentAttemptMapper paymentMapper, ShopOrderMapper orderMapper) {
        this.currentUserService = currentUserService;
        this.paymentMapper = paymentMapper;
        this.orderMapper = orderMapper;
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public PaymentAttempt requireOwned(Authentication authentication, Long id) {
        validateId(id);
        Long userId = currentUserService.requireUserId(authentication);
        PaymentAttempt payment = paymentMapper.selectById(id);
        if (payment == null) { throw new PaymentNotFoundException(); }
        QueryWrapper<ShopOrder> query = new QueryWrapper<>();
        query.eq("id", payment.getOrderId()).eq("user_id", userId);
        if (orderMapper.selectOne(query) == null) { throw new PaymentNotFoundException(); }
        return payment;
    }

    public static void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("支付单编号必须为正整数");
        }
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public List<PaymentResponse> listForOwnedOrder(Authentication authentication, Long orderId) {
        validateId(orderId);
        Long userId = currentUserService.requireUserId(authentication);
        QueryWrapper<ShopOrder> oq = new QueryWrapper<>();
        oq.eq("id", orderId).eq("user_id", userId);
        if (orderMapper.selectOne(oq) == null) { throw new OrderNotFoundException(); }
        QueryWrapper<PaymentAttempt> pq = new QueryWrapper<>();
        pq.eq("order_id", orderId).orderByDesc("id").last("LIMIT 20");
        return paymentMapper.selectList(pq).stream().map(PaymentResponse::from).toList();
    }
}
