package com.wwzyhy.shopagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record KnowledgeRequest(
        @NotBlank @Size(max = 1000) String question,
        @Positive Long productId
) {
}
