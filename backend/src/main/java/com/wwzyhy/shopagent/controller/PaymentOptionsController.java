package com.wwzyhy.shopagent.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentOptionsController {
    private final boolean mock;
    private final boolean sandbox;
    public PaymentOptionsController(@Value("${shopagent.payments.mock.enabled:false}") boolean mock,
            @Value("${shopagent.alipay.sandbox.enabled:false}") boolean sandbox) {
        this.mock = mock; this.sandbox = sandbox;
    }
    @GetMapping("/api/payments/options")
    public Map<String, Object> options() {
        return Map.of("data", Map.of("mockEnabled", mock, "alipaySandboxEnabled", sandbox));
    }
}
