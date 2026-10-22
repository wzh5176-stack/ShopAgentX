package com.wwzyhy.shopagent.controller;

import com.wwzyhy.shopagent.dto.RegisterRequest;
import com.wwzyhy.shopagent.dto.UserResponse;
import com.wwzyhy.shopagent.service.AuthService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, UserResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        UserResponse data = authService.register(request);

        return ResponseEntity.status(201)
                .body(Map.of("data", data));
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        String username = authentication.getName();

        return Map.of("data", Map.of("username", username));
    }
}