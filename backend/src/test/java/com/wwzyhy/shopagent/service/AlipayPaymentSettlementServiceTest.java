package com.wwzyhy.shopagent.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.wwzyhy.shopagent.dto.*;
import com.wwzyhy.shopagent.dto.AlipayQueryResult.TradeStatus;
import com.wwzyhy.shopagent.entity.*;
import com.wwzyhy.shopagent.exception.PaymentConflictException;
import com.wwzyhy.shopagent.exception.PaymentNotFoundException;
import com.wwzyhy.shopagent.mapper.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// 此处验证结算分支和调用次数；数据库事务/锁的证据由 mysql-it 提供。
class AlipayPaymentSettlementServiceTest {
    private final CurrentUserService users = mock(CurrentUserService.class);
    private final PaymentAttemptMapper payments = mock(PaymentAttemptMapper.class);
    private final ShopOrderMapper orders = mock(ShopOrderMapper.class);
    private final OrderItemMapper items = mock(OrderItemMapper.class);
    private final ProductInventoryMapper stocks = mock(ProductInventoryMapper.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    private final AlipayPaymentSettlementService service = new AlipayPaymentSettlementService(users, payments, orders, items, stocks, clock);
    private PaymentAttempt payment;
    private ShopOrder order;

    @BeforeEach void arrange() {
        payment = new PaymentAttempt();
        payment.setId(10L); payment.setOrderId(20L); payment.setChannel("ALIPAY_SANDBOX");
        payment.setStatus("PENDING"); payment.setAmount(new BigDecimal("39.80"));
        payment.setOutTradeNo("test_trade_10"); payment.setProviderAppId("test_app"); payment.setProviderSellerId("test_seller");
        payment.setExpiresAt(LocalDateTime.of(2025, 12, 31, 23, 59));
        order = new ShopOrder(); order.setId(20L); order.setUserId(7L);
        order.setStatus("PENDING_PAYMENT"); order.setTotalAmount(new BigDecimal("39.80"));
        when(users.requireUserId(null)).thenReturn(7L);
        when(payments.selectById(10L)).thenReturn(payment);
        when(payments.selectOne(any(Wrapper.class))).thenReturn(payment);
        when(orders.selectOne(any(Wrapper.class))).thenReturn(order);
    }

    private AlipayQueryResult result(TradeStatus status, String amount) {
        return new AlipayQueryResult(10L, 20L, "test_trade_10", "test_app", "test_seller", status,
                new BigDecimal(amount), "provider_test_10", LocalDateTime.of(2025, 12, 31, 23, 58));
    }

    @ParameterizedTest @EnumSource(value = TradeStatus.class, names = {"NOT_FOUND", "WAIT_BUYER_PAY", "TRADE_CLOSED"})
    void unprovenSuccessKeepsReservation(TradeStatus status) {
        assertEquals(PaymentStatus.PENDING, service.apply(null, 10L, result(status, "39.80")).status());
        verifyNoInteractions(stocks, items);
        verify(orders, never()).update(isNull(), any(Wrapper.class));
    }

    @Test void wrongAmountDoesNotTouchStock() {
        assertThrows(PaymentConflictException.class, () -> service.apply(null, 10L, result(TradeStatus.TRADE_SUCCESS, "0.01")));
        verifyNoInteractions(stocks, items);
    }

    @Test void anotherUsersOrderIsNotVisible() {
        when(orders.selectOne(any(Wrapper.class))).thenReturn(null);
        assertThrows(PaymentNotFoundException.class, () -> service.apply(null, 10L, result(TradeStatus.TRADE_SUCCESS, "39.80")));
        verifyNoInteractions(stocks);
    }

    @Test void duplicateSuccessDoesNotConsumeAgain() {
        payment.setStatus("SUCCEEDED"); payment.setProviderTradeNo("provider_test_10"); order.setStatus("PAID");
        assertEquals(PaymentStatus.SUCCEEDED, service.apply(null, 10L, result(TradeStatus.TRADE_SUCCESS, "39.80")).status());
        verifyNoInteractions(stocks, items);
    }

    @Test void lateConfirmedSuccessIsBookedDespiteLocalDeadline() {
        OrderItem item = new OrderItem(); item.setProductId(9L); item.setQuantity(2);
        when(items.selectList(any(Wrapper.class))).thenReturn(List.of(item));
        when(stocks.consume(9L, 2)).thenReturn(1);
        when(orders.update(isNull(), any(Wrapper.class))).thenReturn(1);
        when(payments.update(isNull(), any(Wrapper.class))).thenAnswer(invocation -> { payment.setStatus("SUCCEEDED"); return 1; });
        assertEquals(PaymentStatus.SUCCEEDED, service.apply(null, 10L, result(TradeStatus.TRADE_SUCCESS, "39.80")).status());
        verify(stocks, times(1)).consume(9L, 2);
    }

    @Test void failedStockConsumptionStopsSettlement() {
        OrderItem item = new OrderItem(); item.setProductId(9L); item.setQuantity(2);
        when(items.selectList(any(Wrapper.class))).thenReturn(List.of(item));
        assertThrows(IllegalStateException.class, () -> service.apply(null, 10L, result(TradeStatus.TRADE_SUCCESS, "39.80")));
        verify(payments, never()).update(isNull(), any(Wrapper.class));
        verify(orders, never()).update(isNull(), any(Wrapper.class));
    }
}
