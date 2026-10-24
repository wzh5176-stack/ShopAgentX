package com.wwzyhy.shopagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressWriteRequest(
        @NotBlank(message = "收货人不能为空")
        @Size(max = 50, message = "收货人不能超过50个字符")
        String recipientName,

        @NotBlank(message = "联系电话不能为空")
        @Size(max = 20, message = "联系电话不能超过20个字符")
        @Pattern(
                regexp = "\\+?[0-9]{6,20}",
                message = "联系电话应为数字，可在开头使用+")
        String phone,

        @NotBlank(message = "省份不能为空")
        @Size(max = 50, message = "省份不能超过50个字符")
        String province,

        @NotBlank(message = "城市不能为空")
        @Size(max = 50, message = "城市不能超过50个字符")
        String city,

        @NotBlank(message = "区县不能为空")
        @Size(max = 50, message = "区县不能超过50个字符")
        String district,

        @NotBlank(message = "详细地址不能为空")
        @Size(max = 200, message = "详细地址不能超过200个字符")
        String detailAddress
) {
    public AddressWriteRequest {
        recipientName = clean(recipientName);
        phone = clean(phone);
        province = clean(province);
        city = clean(city);
        district = clean(district);
        detailAddress = clean(detailAddress);
    }

    private static String clean(String value) {
        return value == null ? null : value.strip();
    }
}