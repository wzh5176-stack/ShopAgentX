package com.wwzyhy.shopagent.exception;

import com.wwzyhy.shopagent.controller.AddressController;
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
@RestControllerAdvice(assignableTypes = AddressController.class)
public class AddressExceptionHandler {

    @ExceptionHandler(AddressNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound() {
        return error(404, "ADDRESS_NOT_FOUND", "地址不存在");
    }

    @ExceptionHandler({
            DuplicateKeyException.class,
            PessimisticLockingFailureException.class
    })
    public ResponseEntity<Map<String, Object>> conflicted() {
        return error(
                409,
                "ADDRESS_CONFLICTED",
                "地址发生并发冲突，请刷新后重试");
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