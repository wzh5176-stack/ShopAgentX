package com.wwzyhy.shopagent.controller;

import com.wwzyhy.shopagent.dto.CheckoutPreviewRequest;
import com.wwzyhy.shopagent.dto.CheckoutPreviewResponse;
import com.wwzyhy.shopagent.service.CheckoutService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/preview")
    public Map<String, CheckoutPreviewResponse> preview(
            @Valid @RequestBody CheckoutPreviewRequest request,
            Authentication authentication) {
        CheckoutPreviewResponse data =
                checkoutService.preview(authentication, request);

        return Map.of("data", data);
    }
}