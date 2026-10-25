package com.wwzyhy.shopagent.dto;

import jakarta.validation.constraints.*;

public record InventoryReplenishRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{8,64}") String requestKey,
        @NotNull @Min(1) @Max(1000000) Integer quantity,
        @NotBlank @Size(max = 200) String reason) {
}
