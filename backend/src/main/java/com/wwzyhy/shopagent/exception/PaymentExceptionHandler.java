package com.wwzyhy.shopagent.exception;

import com.wwzyhy.shopagent.controller.AlipayCheckoutController;
import com.wwzyhy.shopagent.controller.AlipayResumeController;
import com.wwzyhy.shopagent.controller.MockPaymentController;
import com.wwzyhy.shopagent.controller.PaymentController;
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
@RestControllerAdvice(assignableTypes = {
        PaymentController.class,
        MockPaymentController.class,
        AlipayCheckoutController.class, AlipayResumeController.class
})
public class PaymentExceptionHandler {

    @ExceptionHandler(AlipayQueryUnavailableException.class)
    public ResponseEntity<Map<String, Object>> alipayUnavailable() {
        return error(503, "ALIPAY_QUERY_UNAVAILABLE",
                "支付宝沙箱查询暂不可用，支付状态保持不变，请稍后重试");
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound() {
        return error(
                404,
                "PAYMENT_NOT_FOUND",
                "支付单不存在");
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<Map<String, Object>> orderNotFound() {
        return error(
                404,
                "ORDER_NOT_FOUND",
                "订单不存在");
    }

    @ExceptionHandler(PaymentConflictException.class)
    public ResponseEntity<Map<String, Object>> conflict(
            PaymentConflictException exception) {
        return error(
                409,
                "PAYMENT_CONFLICTED",
                exception.getMessage());
    }

    @ExceptionHandler({
            DuplicateKeyException.class,
            PessimisticLockingFailureException.class
    })
    public ResponseEntity<Map<String, Object>> contention() {
        return error(
                409,
                "PAYMENT_CONFLICTED",
                "支付发生并发冲突，请查询或使用原请求编号重试");
    }

    @ExceptionHandler(PaymentUnavailableException.class)
    public ResponseEntity<Map<String, Object>> disabled() {
        return error(
                503,
                "PAYMENT_UNAVAILABLE",
                "本地模拟支付未启用");
    }

    private ResponseEntity<Map<String, Object>> error(
            int status,
            String code,
            String message) {

        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "error",
                        Map.of(
                                "code", code,
                                "message", message)));
    }
}
