package com.wwzyhy.shopagent.controller;

import com.wwzyhy.shopagent.dto.AlipayCheckoutResponse;
import com.wwzyhy.shopagent.dto.PaymentCreateRequest;
import com.wwzyhy.shopagent.service.AlipayCheckoutService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments/alipay")
@ConditionalOnProperty(
        name = "shopagent.alipay.sandbox.enabled",
        havingValue = "true")
public class AlipayCheckoutController {

    private final AlipayCheckoutService service;

    public AlipayCheckoutController(
            AlipayCheckoutService service) {
        this.service = service;
    }

    @PostMapping("/page")
    public ResponseEntity<Map<String, AlipayCheckoutResponse>> page(
            @Valid @RequestBody PaymentCreateRequest request,
            Authentication authentication) {

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of(
                        "data",
                        service.checkout(authentication, request)));
    }
}