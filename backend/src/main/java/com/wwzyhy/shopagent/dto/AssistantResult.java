package com.wwzyhy.shopagent.dto;

import com.wwzyhy.shopagent.entity.Product;
import java.util.List;

public record AssistantResult(
        AiIntentData intent,
        String answer,
        List<Product> products
) {
}