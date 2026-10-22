package com.wwzyhy.shopagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IntentRequest(
        @NotBlank(message = "问题不能为空")
        @Size(max = 1000, message = "问题最多1000个字符")
        String question
) {
}