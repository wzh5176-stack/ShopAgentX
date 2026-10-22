package com.wwzyhy.shopagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ConversationTurnRequest(
        @NotBlank(message = "消息不能为空")
        @Size(max = 1000, message = "消息最多1000个字符")
        String content,

        @Positive(message = "来源消息编号必须为正整数")
        Long sourceMessageId
) {
}