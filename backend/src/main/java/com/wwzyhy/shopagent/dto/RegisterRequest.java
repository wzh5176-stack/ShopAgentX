package com.wwzyhy.shopagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Pattern(
                regexp = "[a-z][a-z0-9_]{2,31}",
                message = "用户名须为3到32位，以小写字母开头，只能包含小写字母、数字和下划线"
        )
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 64, message = "密码长度须为8到64个字符")
        String password
) {
}