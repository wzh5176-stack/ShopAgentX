package com.wwzyhy.shopagent.controller;

import com.wwzyhy.shopagent.dto.PaymentCreateRequest;
import com.wwzyhy.shopagent.dto.PaymentCreateResult;
import com.wwzyhy.shopagent.dto.PaymentResponse;
import com.wwzyhy.shopagent.service.PaymentService;
import com.wwzyhy.shopagent.service.PaymentWriteService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentWriteService writeService;
    private final PaymentService service;

    public PaymentController(PaymentWriteService writeService, PaymentService service) {
        this.writeService = writeService;
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Map<String, PaymentResponse>> create(
            @Valid @RequestBody PaymentCreateRequest request, Authentication authentication) {
        PaymentCreateResult result = writeService.create(authentication, request);
        URI location = URI.create("/api/payments/" + result.data().id());
        return result.created()
                ? ResponseEntity.created(location).body(Map.of("data", result.data()))
                : ResponseEntity.ok().location(location).body(Map.of("data", result.data()));
    }

    @GetMapping("/{id}")
    public Map<String, PaymentResponse> get(@PathVariable("id") Long id, Authentication authentication) {
        return Map.of("data", service.get(authentication, id));
    }

    @PostMapping("/{id}/refresh")
    public Map<String, PaymentResponse> refresh(@PathVariable("id") Long id, Authentication authentication) {
        return Map.of("data", service.refresh(authentication, id));
    }
}
