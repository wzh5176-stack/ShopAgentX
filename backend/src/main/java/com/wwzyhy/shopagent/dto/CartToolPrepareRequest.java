package com.wwzyhy.shopagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CartToolPrepareRequest(
        @NotNull(message = "来源消息编号不能为空")
        @Positive(message = "来源消息编号必须为正整数")
        Long sourceMessageId,

        @NotBlank(message = "操作描述不能为空")
        @Size(max = 1000, message = "操作描述最多1000个字符")
        String question
) {
}