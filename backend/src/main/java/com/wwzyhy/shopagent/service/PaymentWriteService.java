package com.wwzyhy.shopagent.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wwzyhy.shopagent.dto.OrderStatus;
import com.wwzyhy.shopagent.dto.PaymentCreateRequest;
import com.wwzyhy.shopagent.dto.PaymentCreateResult;
import com.wwzyhy.shopagent.dto.PaymentResponse;
import com.wwzyhy.shopagent.dto.PaymentStatus;
import com.wwzyhy.shopagent.entity.PaymentAttempt;
import com.wwzyhy.shopagent.entity.ShopOrder;
import com.wwzyhy.shopagent.exception.OrderNotFoundException;
import com.wwzyhy.shopagent.exception.PaymentConflictException;
import com.wwzyhy.shopagent.exception.PaymentUnavailableException;
import com.wwzyhy.shopagent.mapper.PaymentAttemptMapper;
import com.wwzyhy.shopagent.mapper.ShopOrderMapper;
import jakarta.validation.Validator;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentWriteService {
    private final CurrentUserService currentUserService;
    private final ShopOrderMapper orderMapper;
    private final PaymentAttemptMapper paymentMapper;
    private final Validator validator;
    private final Clock clock;
    private final boolean mockEnabled;

    public PaymentWriteService(CurrentUserService currentUserService, ShopOrderMapper orderMapper,
            PaymentAttemptMapper paymentMapper, Validator validator, Clock clock,
            @Value("${shopagent.payments.mock.enabled:false}") boolean mockEnabled) {
        this.currentUserService = currentUserService;
        this.orderMapper = orderMapper;
        this.paymentMapper = paymentMapper;
        this.validator = validator;
        this.clock = clock;
        this.mockEnabled = mockEnabled;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentCreateResult create(Authentication authentication, PaymentCreateRequest request) {
        if (request == null || !validator.validate(request).isEmpty()) {
            throw new IllegalArgumentException("支付创建参数不符合要求");
        }
        Long userId = currentUserService.requireUserId(authentication);
        QueryWrapper<ShopOrder> orderQuery = new QueryWrapper<>();
        orderQuery.eq("id", request.orderId()).eq("user_id", userId).last("FOR UPDATE");
        ShopOrder order = orderMapper.selectOne(orderQuery);
        if (order == null) { throw new OrderNotFoundException(); }

        QueryWrapper<PaymentAttempt> sameKey = new QueryWrapper<>();
        sameKey.eq("order_id", order.getId()).eq("request_key", request.requestKey()).last("FOR UPDATE");
        PaymentAttempt old = paymentMapper.selectOne(sameKey);
        if (old != null) { return new PaymentCreateResult(false, PaymentResponse.from(old)); }

        if (!mockEnabled) { throw new PaymentUnavailableException(); }
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
        if (!OrderStatus.PENDING_PAYMENT.name().equals(order.getStatus())
                || !now.isBefore(order.getExpiresAt())) {
            throw new PaymentConflictException("订单已关闭、已支付或已超时，不能创建支付");
        }
        AlipayCloseFence.requirePaymentCreationAllowed(paymentMapper, order.getId());
        QueryWrapper<PaymentAttempt> active = new QueryWrapper<>();
        active.eq("order_id", order.getId()).in("status", "PENDING", "SUCCEEDED").last("FOR UPDATE");
        if (!paymentMapper.selectList(active).isEmpty()) {
            throw new PaymentConflictException("已有待处理或成功的支付单，请查询原支付单");
        }
        PaymentAttempt payment = new PaymentAttempt();
        payment.setOrderId(order.getId());
        payment.setRequestKey(request.requestKey());
        payment.setChannel("MOCK");
        payment.setStatus(PaymentStatus.PENDING.name());
        payment.setAmount(order.getTotalAmount());
        payment.setExpiresAt(order.getExpiresAt());
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);
        if (paymentMapper.insert(payment) != 1 || payment.getId() == null) {
            throw new IllegalStateException("创建支付单失败");
        }
        QueryWrapper<PaymentAttempt> saved = new QueryWrapper<>();
        saved.eq("id", payment.getId()).last("FOR SHARE");
        PaymentAttempt row = paymentMapper.selectOne(saved);
        if (row == null) { throw new IllegalStateException("读取支付单失败"); }
        return new PaymentCreateResult(true, PaymentResponse.from(row));
    }
}
