package com.wwzyhy.shopagent.dto;

import java.util.List;

public record MessagePageResponse(
        List<MessageResponse> items,
        Long nextBeforeId,
        boolean hasMore
) {
}