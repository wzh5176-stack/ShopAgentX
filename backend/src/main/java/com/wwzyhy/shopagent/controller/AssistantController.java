package com.wwzyhy.shopagent.controller;

import com.wwzyhy.shopagent.dto.AssistantResponse;
import com.wwzyhy.shopagent.dto.AssistantResult;
import com.wwzyhy.shopagent.dto.IntentRequest;
import com.wwzyhy.shopagent.dto.ProductSummaryResponse;
import com.wwzyhy.shopagent.entity.Product;
import com.wwzyhy.shopagent.service.AssistantService;
import jakarta.validation.Valid;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AssistantController {

    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/api/assistant/ask")
    public Map<String, AssistantResponse> ask(
            @Valid @RequestBody IntentRequest request) {

        AssistantResult result =
                assistantService.ask(request.question());

        List<ProductSummaryResponse> products = new ArrayList<>();
        for (Product product : result.products()) {
            products.add(new ProductSummaryResponse(
                    product.getId(),
                    product.getTitle(),
                    product.getBrand(),
                    product.getPrice()
                            .setScale(2, RoundingMode.UNNECESSARY)
                            .toPlainString(),
                    product.getImageUrl()
            ));
        }

        AssistantResponse data = new AssistantResponse(
                result.intent().keyword(),
                result.intent().maxPrice(),
                result.answer(),
                products
        );
        return Map.of("data", data);
    }
}