package com.wwzyhy.shopagent.controller;

import com.wwzyhy.shopagent.dto.CartToolPrepareRequest;
import com.wwzyhy.shopagent.dto.CartToolPrepareResponse;
import com.wwzyhy.shopagent.service.CartToolPrepareService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conversations/{conversationId}/cart-actions")
public class CartToolController {

    private final CartToolPrepareService cartToolPrepareService;

    public CartToolController(CartToolPrepareService cartToolPrepareService) {
        this.cartToolPrepareService = cartToolPrepareService;
    }

    @PostMapping("/prepare")
    public Map<String, CartToolPrepareResponse> prepare(
            @PathVariable("conversationId") Long conversationId,
            @Valid @RequestBody CartToolPrepareRequest request,
            Authentication authentication) {

        CartToolPrepareResponse data = cartToolPrepareService.prepare(
                authentication,
                conversationId,
                request);

        return Map.of("data", data);
    }
}