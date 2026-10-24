package com.wwzyhy.shopagent.dto;

import com.wwzyhy.shopagent.entity.ShopOrder;

public record OrderAddressResponse(
        String recipientName,
        String phone,
        String province,
        String city,
        String district,
        String detailAddress
) {
    public static OrderAddressResponse from(ShopOrder order) {
        return new OrderAddressResponse(
                order.getRecipientName(),
                order.getPhone(),
                order.getProvince(),
                order.getCity(),
                order.getDistrict(),
                order.getDetailAddress());
    }
}