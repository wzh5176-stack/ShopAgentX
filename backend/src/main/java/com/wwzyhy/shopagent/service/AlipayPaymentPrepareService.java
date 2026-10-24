package com.wwzyhy.shopagent.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wwzyhy.shopagent.dto.OrderStatus;
import com.wwzyhy.shopagent.dto.PaymentCreateRequest;
import com.wwzyhy.shopagent.dto.PaymentStatus;
import com.wwzyhy.shopagent.entity.PaymentAttempt;
import com.wwzyhy.shopagent.entity.ShopOrder;
import com.wwzyhy.shopagent.exception.OrderNotFoundException;
import com.wwzyhy.shopagent.exception.PaymentConflictException;
import com.wwzyhy.shopagent.mapper.PaymentAttemptMapper;
import com.wwzyhy.shopagent.mapper.ShopOrderMapper;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(
        name = "shopagent.alipay.sandbox.enabled",
        havingValue = "true")
public class AlipayPaymentPrepareService {

    private final CurrentUserService currentUserService;
    private final ShopOrderMapper orderMapper;
    private final PaymentAttemptMapper paymentMapper;
    private final Validator validator;
    private final Clock clock;
    private final String appId;
    private final String sellerId;

    public AlipayPaymentPrepareService(
            CurrentUserService currentUserService,
            ShopOrderMapper orderMapper,
            PaymentAttemptMapper paymentMapper,
            Validator validator,
            Clock clock,
            @Value("${shopagent.alipay.sandbox.app-id}")
            String appId,
            @Value("${shopagent.alipay.sandbox.seller-id}")
            String sellerId) {

        if (!appId.matches("[0-9]{1,32}")
                || !sellerId.matches("[0-9]{1,32}")) {
            throw new IllegalArgumentException(
                    "请填写正确的沙箱APPID和商家PID");
        }

        this.currentUserService = currentUserService;
        this.orderMapper = orderMapper;
        this.paymentMapper = paymentMapper;
        this.validator = validator;
        this.clock = clock;
        this.appId = appId;
        this.sellerId = sellerId;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentAttempt prepare(
            Authentication authentication,
            PaymentCreateRequest request) {

        if (request == null
                || !validator.validate(request).isEmpty()) {
            throw new IllegalArgumentException(
                    "支付参数不符合要求");
        }

        Long userId =
                currentUserService.requireUserId(authentication);

        QueryWrapper<ShopOrder> orderQuery =
                new QueryWrapper<>();

        orderQuery.eq("id", request.orderId())
                .eq("user_id", userId)
                .last("FOR UPDATE");

        ShopOrder order = orderMapper.selectOne(orderQuery);

        if (order == null) {
            throw new OrderNotFoundException();
        }
        AlipayCloseFence.requirePaymentCreationAllowed(paymentMapper, order.getId());

        LocalDateTime now = LocalDateTime.now(clock)
                .truncatedTo(ChronoUnit.MILLIS);

        if (!OrderStatus.PENDING_PAYMENT.name()
                .equals(order.getStatus())
                || !order.getExpiresAt()
                .isAfter(now.plusSeconds(61))) {
            throw new PaymentConflictException(
                    "订单当前不能发起沙箱付款");
        }

        if (order.getTotalAmount()
                .compareTo(new BigDecimal("0.01")) < 0
                || order.getTotalAmount()
                .compareTo(new BigDecimal("100000000")) > 0) {
            throw new PaymentConflictException(
                    "订单金额超出沙箱支付范围");
        }

        QueryWrapper<PaymentAttempt> sameKey =
                new QueryWrapper<>();

        sameKey.eq("order_id", order.getId())
                .eq("request_key", request.requestKey())
                .last("FOR UPDATE");

        PaymentAttempt old =
                paymentMapper.selectOne(sameKey);

        if (old != null) {
            if (!"ALIPAY_SANDBOX".equals(old.getChannel())
                    || !appId.equals(old.getProviderAppId())
                    || !sellerId.equals(old.getProviderSellerId())) {
                throw new PaymentConflictException(
                        "原支付请求属于其他渠道或应用");
            }

            if (!PaymentStatus.PENDING.name()
                    .equals(old.getStatus())
                    || !old.getExpiresAt()
                    .isAfter(now.plusSeconds(61))) {
                throw new PaymentConflictException(
                        "原支付尝试已结束或即将到期");
            }

            return old;
        }

        QueryWrapper<PaymentAttempt> active =
                new QueryWrapper<>();

        active.eq("order_id", order.getId())
                .in("status", "PENDING", "SUCCEEDED")
                .last("FOR UPDATE");

        if (!paymentMapper.selectList(active).isEmpty()) {
            throw new PaymentConflictException(
                    "已有待处理或成功支付，请查询原支付单");
        }

        PaymentAttempt payment = new PaymentAttempt();

        payment.setOrderId(order.getId());
        payment.setRequestKey(request.requestKey());
        payment.setChannel("ALIPAY_SANDBOX");
        payment.setStatus(PaymentStatus.PENDING.name());

        payment.setAmount(order.getTotalAmount());
        payment.setExpiresAt(order.getExpiresAt());
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);

        payment.setOutTradeNo(
                "SA_" + UUID.randomUUID()
                        .toString()
                        .replace("-", ""));

        payment.setProviderAppId(appId);
        payment.setProviderSellerId(sellerId);

        if (paymentMapper.insert(payment) != 1
                || payment.getId() == null) {
            throw new IllegalStateException(
                    "保存官方沙箱支付单失败");
        }

        QueryWrapper<PaymentAttempt> saved =
                new QueryWrapper<>();

        saved.eq("id", payment.getId())
                .last("FOR SHARE");

        PaymentAttempt row =
                paymentMapper.selectOne(saved);

        if (row == null) {
            throw new IllegalStateException(
                    "读取官方支付单失败");
        }

        return row;
    }
}
