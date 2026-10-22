package com.wwzyhy.shopagent.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wwzyhy.shopagent.dto.AiCartToolIntentData;
import com.wwzyhy.shopagent.dto.AiCartToolIntentRequest;
import com.wwzyhy.shopagent.dto.AiCartToolProduct;
import com.wwzyhy.shopagent.dto.CartToolPrepareRequest;
import com.wwzyhy.shopagent.dto.CartToolPrepareResponse;
import com.wwzyhy.shopagent.dto.ProductSummaryResponse;
import com.wwzyhy.shopagent.entity.Product;
import com.wwzyhy.shopagent.exception.CartProductUnavailableException;
import com.wwzyhy.shopagent.mapper.ProductMapper;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class CartToolPrepareService {

    private final RecommendationSourceService recommendationSourceService;
    private final AiClient aiClient;
    private final ProductMapper productMapper;

    public CartToolPrepareService(
            RecommendationSourceService recommendationSourceService,
            AiClient aiClient,
            ProductMapper productMapper) {

        this.recommendationSourceService = recommendationSourceService;
        this.aiClient = aiClient;
        this.productMapper = productMapper;
    }

    public CartToolPrepareResponse prepare(
            Authentication authentication,
            Long conversationId,
            CartToolPrepareRequest request) {

        if (request == null
                || request.question() == null
                || request.question().isBlank()
                || request.question().length() > 1000) {
            throw new IllegalArgumentException("操作描述不符合要求");
        }

        List<ProductSummaryResponse> products =
                recommendationSourceService.loadProducts(
                        authentication,
                        conversationId,
                        request.sourceMessageId());

        List<AiCartToolProduct> aiProducts = new ArrayList<>();
        for (ProductSummaryResponse product : products) {
            aiProducts.add(new AiCartToolProduct(
                    product.title(),
                    product.brand(),
                    product.price()));
        }

        AiCartToolIntentData intent = aiClient.parseCartToolIntent(
                new AiCartToolIntentRequest(
                        request.question().strip(),
                        List.copyOf(aiProducts)));

        if ("none".equals(intent.action())) {
            return new CartToolPrepareResponse(
                    "none",
                    conversationId,
                    request.sourceMessageId(),
                    null,
                    null,
                    "本次没有识别到购物车修改意图。");
        }

        if ("clarify".equals(intent.action())) {
            return new CartToolPrepareResponse(
                    "clarify",
                    conversationId,
                    request.sourceMessageId(),
                    null,
                    null,
                    "请明确商品和最终数量，例如把第一款在购物车中的数量设为2。");
        }

        ProductSummaryResponse selected =
                products.get(intent.productIndex() - 1);

        QueryWrapper<Product> query = new QueryWrapper<>();
        query.eq("id", selected.id());
        query.eq("status", 1);

        Product current = productMapper.selectOne(query);
        if (current == null) {
            throw new CartProductUnavailableException();
        }

        ProductSummaryResponse product = new ProductSummaryResponse(
                current.getId(),
                current.getTitle(),
                current.getBrand(),
                current.getPrice()
                        .setScale(2, RoundingMode.UNNECESSARY)
                        .toPlainString(),
                current.getImageUrl());

        return new CartToolPrepareResponse(
                "set_quantity",
                conversationId,
                request.sourceMessageId(),
                product,
                intent.quantity(),
                "已识别商品和目标数量。");
    }
}