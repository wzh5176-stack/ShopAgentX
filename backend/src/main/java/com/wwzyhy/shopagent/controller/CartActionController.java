package com.wwzyhy.shopagent.controller;

import com.wwzyhy.shopagent.dto.CartActionCreateRequest;
import com.wwzyhy.shopagent.dto.CartActionResponse;
import com.wwzyhy.shopagent.service.CartActionService;
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
import com.wwzyhy.shopagent.dto.CartActionStatus;


@RestController
@RequestMapping("/api/conversations/{conversationId}/cart-actions")
public class CartActionController {

    private final CartActionService cartActionService;

    public CartActionController(CartActionService cartActionService) {
        this.cartActionService = cartActionService;
    }

    @PostMapping
    public ResponseEntity<Map<String, CartActionResponse>> create(
            @PathVariable("conversationId") Long conversationId,
            @Valid @RequestBody CartActionCreateRequest request,
            Authentication authentication) {

        CartActionResponse data = cartActionService.create(
                authentication,
                conversationId,
                request);

        URI location = URI.create(
                "/api/conversations/" + conversationId
                        + "/cart-actions/" + data.operationId());

        return ResponseEntity.created(location)
                .body(Map.of("data", data));
    }

    @GetMapping("/{operationId}")
    public Map<String, CartActionResponse> getOwned(
            @PathVariable("conversationId") Long conversationId,
            @PathVariable("operationId") String operationId,
            Authentication authentication) {

        CartActionResponse data = cartActionService.getOwned(
                authentication,
                conversationId,
                operationId);

        return Map.of("data", data);
    }


    @PostMapping("/{operationId}/confirm")
    public ResponseEntity<Map<String, Object>> confirm(
            @PathVariable("conversationId") Long conversationId,
            @PathVariable("operationId") String operationId,
            Authentication authentication) {

        CartActionResponse data = cartActionService.confirm(
                authentication,
                conversationId,
                operationId);

        return actionResponse(data, CartActionStatus.SUCCEEDED);
    }

    @PostMapping("/{operationId}/cancel")
    public ResponseEntity<Map<String, Object>> cancel(
            @PathVariable("conversationId") Long conversationId,
            @PathVariable("operationId") String operationId,
            Authentication authentication) {

        CartActionResponse data = cartActionService.cancel(
                authentication,
                conversationId,
                operationId);

        return actionResponse(data, CartActionStatus.CANCELLED);
    }

    private ResponseEntity<Map<String, Object>> actionResponse(
            CartActionResponse data,
            CartActionStatus expectedStatus) {

        if (data.status() == expectedStatus) {
            return ResponseEntity.ok(Map.of("data", data));
        }

        String code;
        String message;

        switch (data.status()) {
            case EXPIRED -> {
                code = "CART_ACTION_EXPIRED";
                message = "操作已过期，请重新创建";
            }
            case CONFLICTED -> {
                code = "CART_ACTION_CONFLICTED";
                message = "购物车数量已变化，请重新创建操作";
            }
            case REJECTED -> {
                code = "PRODUCT_UNAVAILABLE";
                message = "商品已不可用，无法执行该操作";
            }
            case CANCELLED -> {
                code = "CART_ACTION_CANCELLED";
                message = "操作已取消，不能再确认";
            }
            case SUCCEEDED -> {
                code = "CART_ACTION_ALREADY_SUCCEEDED";
                message = "操作已执行，不能取消";
            }
            default -> throw new IllegalStateException("操作没有完成状态处理");
        }

        return ResponseEntity.status(409).body(Map.of(
                "data", data,
                "error", Map.of(
                        "code", code,
                        "message", message)));
    }
}