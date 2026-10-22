package com.wwzyhy.shopagent.controller;

import com.wwzyhy.shopagent.dto.CartItemSetRequest;
import com.wwzyhy.shopagent.dto.CartItemSetResponse;
import com.wwzyhy.shopagent.dto.CartResponse;
import com.wwzyhy.shopagent.entity.CartItem;
import com.wwzyhy.shopagent.service.CartService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public Map<String, CartResponse> list(
            Authentication authentication) {

        CartResponse data = cartService.list(authentication);

        return Map.of("data", data);
    }

    @PutMapping("/items/{productId}")
    public Map<String, CartItemSetResponse> setQuantity(
            @PathVariable("productId") Long productId,
            @Valid @RequestBody CartItemSetRequest request,
            Authentication authentication) {

        CartItem item = cartService.setQuantity(
                authentication,
                productId,
                request.quantity());

        CartItemSetResponse data = new CartItemSetResponse(
                item.getProductId(),
                item.getQuantity());

        return Map.of("data", data);
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Void> remove(
            @PathVariable("productId") Long productId,
            Authentication authentication) {

        cartService.remove(authentication, productId);

        return ResponseEntity.noContent().build();
    }
}