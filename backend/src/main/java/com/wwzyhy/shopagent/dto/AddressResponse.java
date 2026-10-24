package com.wwzyhy.shopagent.dto;

import com.wwzyhy.shopagent.entity.UserAddress;
import java.time.LocalDateTime;

public record AddressResponse(
        Long id,
        String recipientName,
        String phone,
        String province,
        String city,
        String district,
        String detailAddress,
        boolean defaultAddress,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AddressResponse from(UserAddress address) {
        return new AddressResponse(
                address.getId(),
                address.getRecipientName(),
                address.getPhone(),
                address.getProvince(),
                address.getCity(),
                address.getDistrict(),
                address.getDetailAddress(),
                Integer.valueOf(1).equals(address.getDefaultFlag()),
                address.getCreatedAt(),
                address.getUpdatedAt());
    }
}