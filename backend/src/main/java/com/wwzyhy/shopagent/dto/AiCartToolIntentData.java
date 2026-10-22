package com.wwzyhy.shopagent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiCartToolIntentData(
        @JsonProperty(value = "action", required = true)
        String action,

        @JsonProperty(value = "productIndex", required = true)
        Integer productIndex,

        @JsonProperty(value = "quantity", required = true)
        Integer quantity
) {
}