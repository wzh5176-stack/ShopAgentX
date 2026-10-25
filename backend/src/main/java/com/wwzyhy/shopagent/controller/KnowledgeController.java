package com.wwzyhy.shopagent.controller;

import com.wwzyhy.shopagent.dto.AiKnowledgeData;
import com.wwzyhy.shopagent.dto.KnowledgeRequest;
import com.wwzyhy.shopagent.service.AiClient;
import com.wwzyhy.shopagent.service.ProductService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class KnowledgeController {

    private final AiClient aiClient;
    private final ProductService productService;

    public KnowledgeController(AiClient aiClient, ProductService productService) {
        this.aiClient = aiClient;
        this.productService = productService;
    }

    @PostMapping("/api/assistant/knowledge")
    public Map<String, AiKnowledgeData> askKnowledge(
            @Valid @RequestBody KnowledgeRequest request) {

        AiKnowledgeData data;
        if (request.productId() == null) {
            data = aiClient.askKnowledge(request.question().trim());
        } else {
            var product = productService.findOnSaleById(request.productId());
            if (product == null) {
                throw new IllegalArgumentException("商品不存在或已下架，请重新选择");
            }
            // 商品身份由数据库读取，不接收浏览器任意指定的资料路径/品牌。
            data = aiClient.askKnowledge(request.question().trim(), product.getTitle(), product.getBrand());
        }

        return Map.of("data", data);
    }
}
