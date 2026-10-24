package com.wwzyhy.shopagent.exception;

import com.wwzyhy.shopagent.controller.CheckoutController;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = CheckoutController.class)
public class CheckoutExceptionHandler {

    @ExceptionHandler(AddressNotFoundException.class)
    public ResponseEntity<Map<String, Object>> addressNotFound() {
        return error(404, "ADDRESS_NOT_FOUND", "地址不存在");
    }

    @ExceptionHandler(CheckoutUnavailableException.class)
    public ResponseEntity<Map<String, Object>> unavailable(
            CheckoutUnavailableException exception) {
        return error(
                409, "CHECKOUT_UNAVAILABLE", exception.getMessage());
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