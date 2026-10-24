package com.wwzyhy.shopagent.exception;

import com.wwzyhy.shopagent.controller.OrderController;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = OrderController.class)
public class OrderExceptionHandler {

    @ExceptionHandler(AlipayCloseUnavailableException.class)
    public ResponseEntity<Map<String, Object>> alipayCloseUnavailable() {
        return error(503, "ALIPAY_CLOSE_UNAVAILABLE", "支付宝沙箱关单暂不可用，库存保持预占，请稍后重试");
    }

    @ExceptionHandler(AlipayQueryUnavailableException.class)
    public ResponseEntity<Map<String, Object>> alipayQueryUnavailable() {
        return error(503, "ALIPAY_QUERY_UNAVAILABLE", "支付宝沙箱查询暂不可用，库存保持预占，请稍后重试");
    }

    @ExceptionHandler(PaymentConflictException.class)
    public ResponseEntity<Map<String, Object>> paymentConflict(PaymentConflictException exception) {
        return error(409, "ORDER_CONFLICTED", exception.getMessage());
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<Map<String, Object>> orderNotFound() {
        return error(404, "ORDER_NOT_FOUND", "订单不存在");
    }

    @ExceptionHandler(AddressNotFoundException.class)
    public ResponseEntity<Map<String, Object>> addressNotFound() {
        return error(404, "ADDRESS_NOT_FOUND", "地址不存在");
    }

    @ExceptionHandler(CheckoutUnavailableException.class)
    public ResponseEntity<Map<String, Object>> unavailable(
            CheckoutUnavailableException exception) {
        return error(409, "CHECKOUT_UNAVAILABLE", exception.getMessage());
    }

    @ExceptionHandler(OrderConflictException.class)
    public ResponseEntity<Map<String, Object>> conflicted(
            OrderConflictException exception) {
        return error(409, "ORDER_CONFLICTED", exception.getMessage());
    }

    @ExceptionHandler({
            DuplicateKeyException.class,
            PessimisticLockingFailureException.class
    })
    public ResponseEntity<Map<String, Object>> databaseConflict() {
        return error(409, "ORDER_CONFLICTED", "订单发生并发冲突，请使用原请求编号重试");
    }

    private ResponseEntity<Map<String, Object>> error(
            int status,
            String code,
            String message) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("error", Map.of(
                        "code", code,
                        "message", message)));
    }
}
