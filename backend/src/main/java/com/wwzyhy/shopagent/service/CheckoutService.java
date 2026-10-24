package com.wwzyhy.shopagent.service;

import com.wwzyhy.shopagent.dto.AddressResponse;
import com.wwzyhy.shopagent.dto.CheckoutItemRequest;
import com.wwzyhy.shopagent.dto.CheckoutItemResponse;
import com.wwzyhy.shopagent.dto.CheckoutPreviewRequest;
import com.wwzyhy.shopagent.dto.CheckoutPreviewResponse;
import com.wwzyhy.shopagent.entity.Product;
import com.wwzyhy.shopagent.entity.ProductInventory;
import com.wwzyhy.shopagent.exception.CheckoutUnavailableException;
import com.wwzyhy.shopagent.mapper.ProductInventoryMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckoutService {

    private final AddressService addressService;
    private final ProductService productService;
    private final ProductInventoryMapper inventoryMapper;
    private final Validator validator;

    public CheckoutService(
            AddressService addressService,
            ProductService productService,
            ProductInventoryMapper inventoryMapper,
            Validator validator) {
        this.addressService = addressService;
        this.productService = productService;
        this.inventoryMapper = inventoryMapper;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public CheckoutPreviewResponse preview(
            Authentication authentication,
            CheckoutPreviewRequest request) {
        validateRequest(request);

        AddressResponse address =
                addressService.get(authentication, request.addressId());

        List<CheckoutItemResponse> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CheckoutItemRequest item : request.items()) {
            Product product =
                    productService.findOnSaleById(item.productId());

            if (product == null) {
                throw new CheckoutUnavailableException(
                        "部分商品不存在或已下架，请刷新后重选");
            }

            ProductInventory inventory =
                    inventoryMapper.selectById(item.productId());

            if (inventory == null
                    || inventory.getAvailableQuantity() < item.quantity()) {
                throw new CheckoutUnavailableException(
                        "部分商品库存不足，请调整购买数量");
            }

            BigDecimal unitPrice = product.getPrice();
            BigDecimal lineAmount = unitPrice.multiply(
                    BigDecimal.valueOf(item.quantity()));

            totalAmount = totalAmount.add(lineAmount);

            items.add(new CheckoutItemResponse(
                    product.getId(),
                    product.getTitle(),
                    product.getBrand(),
                    product.getImageUrl(),
                    money(unitPrice),
                    item.quantity(),
                    money(lineAmount)));
        }

        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CheckoutUnavailableException(
                    "本轮暂不支持总金额为0的订单");
        }

        return new CheckoutPreviewResponse(
                address, items, money(totalAmount));
    }

    private void validateRequest(CheckoutPreviewRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("结算信息不能为空");
        }

        Set<ConstraintViolation<CheckoutPreviewRequest>> violations =
                validator.validate(request);

        if (!violations.isEmpty()) {
            throw new IllegalArgumentException("结算参数不符合要求");
        }

        Set<Long> productIds = new HashSet<>();

        for (CheckoutItemRequest item : request.items()) {
            if (!productIds.add(item.productId())) {
                throw new IllegalArgumentException("同一商品不能重复提交");
            }
        }
    }

    private String money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.UNNECESSARY)
                .toPlainString();
    }
}