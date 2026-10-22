package com.wwzyhy.shopagent.controller;

import java.util.Map;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminCsrfController {

    @GetMapping("/api/admin/csrf")
    public Map<String, Object> csrf(CsrfToken token) {
        return Map.of("data", Map.of(
                "token", token.getToken(),
                "headerName", token.getHeaderName()
        ));
    }
}