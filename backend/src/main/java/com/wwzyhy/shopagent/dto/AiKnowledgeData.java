package com.wwzyhy.shopagent.dto;

import java.util.List;

public record AiKnowledgeData(
        String answer,
        Boolean answerable,
        List<AiKnowledgeSource> sources
) {
}