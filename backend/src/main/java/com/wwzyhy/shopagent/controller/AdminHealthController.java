package com.wwzyhy.shopagent.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminHealthController {

    @GetMapping("/api/admin/health")
    public Map<String, Object> health() {
        return Map.of(
                "data", Map.of(
                        "status", "UP",
                        "role", "ADMIN"
                )
        );
    }
}