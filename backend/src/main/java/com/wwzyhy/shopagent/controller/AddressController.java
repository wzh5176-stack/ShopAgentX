package com.wwzyhy.shopagent.controller;

import com.wwzyhy.shopagent.dto.AddressResponse;
import com.wwzyhy.shopagent.dto.AddressWriteRequest;
import com.wwzyhy.shopagent.service.AddressService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public Map<String, Object> list(Authentication authentication) {
        return Map.of("data", Map.of(
                "items", addressService.list(authentication)));
    }

    @PostMapping
    public ResponseEntity<Map<String, AddressResponse>> create(
            @Valid @RequestBody AddressWriteRequest request,
            Authentication authentication) {
        AddressResponse data =
                addressService.create(authentication, request);

        URI location = URI.create("/api/addresses/" + data.id());

        return ResponseEntity.created(location)
                .body(Map.of("data", data));
    }

    @GetMapping("/{id}")
    public Map<String, AddressResponse> get(
            @PathVariable("id") Long id,
            Authentication authentication) {
        return Map.of("data",
                addressService.get(authentication, id));
    }

    @PutMapping("/{id}")
    public Map<String, AddressResponse> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody AddressWriteRequest request,
            Authentication authentication) {
        return Map.of("data",
                addressService.update(authentication, id, request));
    }

    @PutMapping("/{id}/default")
    public Map<String, AddressResponse> setDefault(
            @PathVariable("id") Long id,
            Authentication authentication) {
        return Map.of("data",
                addressService.setDefault(authentication, id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(
            @PathVariable("id") Long id,
            Authentication authentication) {
        addressService.remove(authentication, id);
        return ResponseEntity.noContent().build();
    }
}