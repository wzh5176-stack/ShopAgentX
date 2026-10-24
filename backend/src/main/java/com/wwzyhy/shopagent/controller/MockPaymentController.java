package com.wwzyhy.shopagent.controller;

import com.wwzyhy.shopagent.dto.MockPaymentRequest;
import com.wwzyhy.shopagent.dto.PaymentResponse;
import com.wwzyhy.shopagent.service.PaymentService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@ConditionalOnProperty(name = "shopagent.payments.mock.enabled", havingValue = "true")
public class MockPaymentController {
    private final PaymentService service;
    public MockPaymentController(PaymentService service) { this.service = service; }

    @PostMapping("/{id}/mock-result")
    public Map<String, PaymentResponse> simulate(@PathVariable("id") Long id,
            @Valid @RequestBody MockPaymentRequest request, Authentication authentication) {
        return Map.of("data", service.simulate(authentication, id, request.outcome()));
    }
}
