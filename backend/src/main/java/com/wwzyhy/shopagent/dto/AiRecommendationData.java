package com.wwzyhy.shopagent.dto;

import java.util.List;

public record AiRecommendationData(
        String answer,
        List<Long> recommendedIds
) {
}