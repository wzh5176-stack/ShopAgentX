package com.wwzyhy.shopagent.controller;

import com.wwzyhy.shopagent.dto.*;
import com.wwzyhy.shopagent.service.InventoryService;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/products/{productId}/inventory")
public class InventoryController {
    private final InventoryService service;
    public InventoryController(InventoryService service) { this.service = service; }

    @GetMapping
    public Map<String, InventoryResponse> get(@PathVariable Long productId) {
        return Map.of("data", service.get(productId));
    }

    @GetMapping("/replenishments")
    public Map<String, Object> history(@PathVariable Long productId) {
        return Map.of("data", Map.of("items", service.history(productId)));
    }

    @PostMapping("/replenishments")
    public Map<String, InventoryReplenishmentResponse> replenish(@PathVariable Long productId,
            @Valid @RequestBody InventoryReplenishRequest request, Authentication authentication) {
        return Map.of("data", service.replenish(productId, request, authentication.getName()));
    }
}
