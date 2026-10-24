package com.wwzyhy.shopagent.controller;

import com.wwzyhy.shopagent.dto.OrderCreateRequest;
import com.wwzyhy.shopagent.dto.OrderCreateResult;
import com.wwzyhy.shopagent.dto.OrderDetailResponse;
import com.wwzyhy.shopagent.dto.OrderPageResponse;
import com.wwzyhy.shopagent.dto.OrderStatus;
import com.wwzyhy.shopagent.service.OrderCloseCoordinator;
import com.wwzyhy.shopagent.service.OrderCreateService;
import com.wwzyhy.shopagent.service.OrderReadService;
import com.wwzyhy.shopagent.service.PaymentReadService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderCreateService createService;
    private final OrderReadService readService;
    private final OrderCloseCoordinator closeService;
    private final PaymentReadService payments;

    public OrderController(
            OrderCreateService createService,
            OrderReadService readService,
            OrderCloseCoordinator closeService, PaymentReadService payments) {
        this.createService = createService;
        this.readService = readService;
        this.closeService = closeService;
        this.payments = payments;
    }

    @PostMapping
    public ResponseEntity<Map<String, OrderDetailResponse>> create(
            @Valid @RequestBody OrderCreateRequest request,
            Authentication authentication) {
        OrderCreateResult result = createService.create(authentication, request);
        URI location = URI.create("/api/orders/" + result.data().id());

        if (result.created()) {
            return ResponseEntity.created(location)
                    .body(Map.of("data", result.data()));
        }

        return ResponseEntity.ok()
                .location(location)
                .body(Map.of("data", result.data()));
    }

    @GetMapping("/{id}")
    public Map<String, OrderDetailResponse> get(
            @PathVariable("id") Long id,
            Authentication authentication) {
        return Map.of("data", readService.get(authentication, id));
    }

    @GetMapping
    public Map<String, OrderPageResponse> list(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "status", required = false) OrderStatus status,
            Authentication authentication) {
        return Map.of("data", readService.list(authentication, page, size, status));
    }

    @PostMapping("/{id}/cancel")
    public Map<String, OrderDetailResponse> cancel(
            @PathVariable("id") Long id,
            Authentication authentication) {
        return Map.of("data", closeService.cancel(authentication, id));
    }

    @GetMapping("/{id}/payments")
    public Map<String, Object> payments(@PathVariable("id") Long id, Authentication authentication) {
        return Map.of("data", Map.of("items", payments.listForOwnedOrder(authentication, id)));
    }
}
