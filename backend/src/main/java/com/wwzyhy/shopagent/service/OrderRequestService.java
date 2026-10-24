package com.wwzyhy.shopagent.service;

import com.wwzyhy.shopagent.dto.CheckoutItemRequest;
import com.wwzyhy.shopagent.dto.OrderCreateRequest;
import com.wwzyhy.shopagent.dto.PreparedOrderRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class OrderRequestService {

    private final Validator validator;

    public OrderRequestService(Validator validator) {
        this.validator = validator;
    }

    public PreparedOrderRequest prepare(OrderCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("下单信息不能为空");
        }

        Set<ConstraintViolation<OrderCreateRequest>> violations =
                validator.validate(request);

        if (!violations.isEmpty()) {
            throw new IllegalArgumentException("下单参数不符合要求");
        }

        List<CheckoutItemRequest> items =
                new ArrayList<>(request.items());

        items.sort(Comparator.comparing(CheckoutItemRequest::productId));

        Set<Long> productIds = new HashSet<>();
        StringBuilder content = new StringBuilder();

        content.append(request.addressId()).append('|');

        for (CheckoutItemRequest item : items) {
            if (!productIds.add(item.productId())) {
                throw new IllegalArgumentException("同一商品不能重复提交");
            }

            content.append(item.productId())
                    .append(':')
                    .append(item.quantity())
                    .append(';');
        }

        return new PreparedOrderRequest(
                request.requestKey(),
                request.addressId(),
                items,
                sha256(content.toString()));
    }

    private String sha256(String content) {
        try {
            byte[] result = MessageDigest.getInstance("SHA-256")
                    .digest(content.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(result);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "当前环境不支持SHA-256", exception);
        }
    }
}