package com.wwzyhy.shopagent.dto;

import com.wwzyhy.shopagent.entity.Product;
import java.util.List;

public record CandidateSearchResult(
        AiIntentData intent,
        List<Product> products
) {
}