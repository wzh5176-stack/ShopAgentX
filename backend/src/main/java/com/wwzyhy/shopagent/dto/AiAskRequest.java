package com.wwzyhy.shopagent.dto;

import java.util.List;

public record AiAskRequest(
        String question,
        List<AiCandidateProduct> products
) {
}