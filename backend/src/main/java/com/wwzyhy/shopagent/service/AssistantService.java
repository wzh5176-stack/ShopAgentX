package com.wwzyhy.shopagent.service;

import com.wwzyhy.shopagent.dto.AiAskRequest;
import com.wwzyhy.shopagent.dto.AiCandidateProduct;
import com.wwzyhy.shopagent.dto.AiIntentData;
import com.wwzyhy.shopagent.dto.AiRecommendationData;
import com.wwzyhy.shopagent.dto.AssistantResult;
import com.wwzyhy.shopagent.dto.CandidateSearchResult;
import com.wwzyhy.shopagent.entity.Product;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AssistantService {

    private final AiClient aiClient;
    private final ProductService productService;

    public AssistantService(AiClient aiClient, ProductService productService) {
        this.aiClient = aiClient;
        this.productService = productService;
    }

    public CandidateSearchResult searchCandidates(String question) {
        return searchCandidates(aiClient.parseIntent(question));
    }

    public CandidateSearchResult searchCandidates(AiIntentData intent) {
        if (intent.keyword().isBlank()) {
            return new CandidateSearchResult(intent, List.of());
        }

        BigDecimal maxPrice = intent.maxPrice() == null
                ? null
                : new BigDecimal(intent.maxPrice());

        List<Product> products = productService.findCandidates(
                intent.keyword(), maxPrice);

        return new CandidateSearchResult(intent, products);
    }

    public AssistantResult ask(String question) {
        return recommendCandidates(
                question, searchCandidates(question), null);
    }

    public AssistantResult askWithIntent(
            String question, AiIntentData intent) {
        return recommendCandidates(
                question, searchCandidates(intent), intent);
    }

    private AssistantResult recommendCandidates(
            String question,
            CandidateSearchResult candidates,
            AiIntentData searchIntent) {

        if (candidates.intent().keyword().isBlank()) {
            return new AssistantResult(
                    candidates.intent(),
                    "请告诉我你想买什么商品，例如跑鞋或手机。",
                    List.of()
            );
        }

        if (candidates.products().isEmpty()) {
            return new AssistantResult(
                    candidates.intent(),
                    "暂时没有符合条件的商品，可以换个关键词或调整预算。",
                    List.of()
            );
        }

        List<AiCandidateProduct> products = new ArrayList<>();
        Map<Long, Product> productsById = new HashMap<>();

        for (Product product : candidates.products()) {
            products.add(new AiCandidateProduct(
                    product.getId(),
                    product.getTitle(),
                    product.getBrand(),
                    product.getPrice()
                            .setScale(2, RoundingMode.UNNECESSARY)
                            .toPlainString(),
                    product.getDescription()
            ));
            productsById.put(product.getId(), product);
        }

        AiRecommendationData recommendation = aiClient.recommend(
                new AiAskRequest(question, products),
                searchIntent
        );

        List<Product> selectedProducts = new ArrayList<>();
        for (Long id : recommendation.recommendedIds()) {
            selectedProducts.add(productsById.get(id));
        }

        if (selectedProducts.isEmpty()) {
            return new AssistantResult(
                    candidates.intent(),
                    "当前候选中暂无适合推荐的商品，可以调整需求后再试。",
                    List.of()
            );
        }

        return new AssistantResult(
                candidates.intent(),
                recommendation.answer(),
                selectedProducts
        );
    }
}