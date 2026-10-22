package com.wwzyhy.shopagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConversationCreateRequest(
        @NotBlank(message = "会话标题不能为空")
        @Size(max = 100, message = "会话标题不能超过100个字符")
        String title
) {
}