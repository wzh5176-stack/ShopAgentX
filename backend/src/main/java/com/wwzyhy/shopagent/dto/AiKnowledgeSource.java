package com.wwzyhy.shopagent.dto;

public record AiKnowledgeSource(
        String chunkId,
        String source,
        String document,
        String section,
        String content
) {
}