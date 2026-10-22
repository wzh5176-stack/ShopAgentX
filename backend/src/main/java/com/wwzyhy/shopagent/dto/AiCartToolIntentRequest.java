package com.wwzyhy.shopagent.dto;

import java.util.List;

public record AiCartToolIntentRequest(
        String question,
        List<AiCartToolProduct> products
) {
}