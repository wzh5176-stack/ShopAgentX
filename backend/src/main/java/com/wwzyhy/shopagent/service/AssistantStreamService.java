package com.wwzyhy.shopagent.service;

import com.wwzyhy.shopagent.dto.AiAskRequest;
import com.wwzyhy.shopagent.dto.AiCandidateProduct;
import com.wwzyhy.shopagent.dto.AiChatData;
import com.wwzyhy.shopagent.dto.AiIntentData;
import com.wwzyhy.shopagent.dto.AiStreamDelta;
import com.wwzyhy.shopagent.dto.AssistantResult;
import com.wwzyhy.shopagent.dto.CandidateSearchResult;
import com.wwzyhy.shopagent.entity.Product;
import java.io.InputStream;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
public class AssistantStreamService {

    private final AssistantService assistantService;
    private final AiClient aiClient;
    private final AiStreamJsonCodec codec;

    public AssistantStreamService(
            AssistantService assistantService,
            AiClient aiClient,
            AiStreamJsonCodec codec) {
        this.assistantService = assistantService;
        this.aiClient = aiClient;
        this.codec = codec;
    }

    public AssistantResult recommend(
            String question,
            AiIntentData intent,
            Consumer<AiStreamDelta> onDelta,
            Consumer<InputStream> onOpen) {

        if (question == null || question.isBlank() || question.length() > 1000
                || intent == null || intent.keyword() == null
                || intent.keyword().length() > 50
                || (intent.maxPrice() != null
                && !intent.maxPrice().matches("(0|[1-9][0-9]{0,7})\\.[0-9]{2}"))
                || onDelta == null || onOpen == null) {
            throw new IllegalArgumentException("流式导购参数不符合约定");
        }

        checkCancelled();
        CandidateSearchResult candidates = assistantService.searchCandidates(intent);
        if (intent.keyword().isBlank()) {
            return new AssistantResult(intent,
                    "请告诉我你想买什么商品，例如跑鞋或手机。", List.of());
        }
        if (candidates.products().isEmpty()) {
            return new AssistantResult(intent,
                    "暂时没有符合条件的商品，可以换个关键词或调整预算。", List.of());
        }

        List<AiCandidateProduct> allCandidates = new ArrayList<>();
        Map<Long, Product> productsById = new HashMap<>();
        for (Product product : candidates.products()) {
            allCandidates.add(toCandidate(product));
            productsById.put(product.getId(), product);
        }

        checkCancelled();
        var selection = aiClient.selectProducts(
                new AiAskRequest(question, allCandidates), intent);
        checkCancelled();

        List<Product> selectedProducts = new ArrayList<>();
        List<AiCandidateProduct> selectedCandidates = new ArrayList<>();
        for (Long id : selection.recommendedIds()) {
            Product product = productsById.get(id);
            if (product == null) {
                throw new RestClientException("选择结果没有对应的真实商品");
            }
            selectedProducts.add(product);
            selectedCandidates.add(toCandidate(product));
        }

        if (selectedProducts.isEmpty()) {
            return new AssistantResult(intent,
                    "当前候选中暂无适合推荐的商品，可以调整需求后再试。", List.of());
        }

        String answer = readExplanation(
                new AiAskRequest(question, selectedCandidates),
                intent, onDelta, onOpen);
        return new AssistantResult(intent, answer, List.copyOf(selectedProducts));
    }

    private String readExplanation(
            AiAskRequest request,
            AiIntentData intent,
            Consumer<AiStreamDelta> onDelta,
            Consumer<InputStream> onOpen) {

        StringBuilder answer = new StringBuilder();
        AtomicReference<AiChatData> completed = new AtomicReference<>();
        checkCancelled();

        aiClient.streamRecommendation(request, intent, event -> {
            checkCancelled();
            switch (event.event()) {
                case "delta" -> {
                    AiStreamDelta data = codec.readDelta(event.data());
                    if (answer.length() + data.content().length() > 2000) {
                        throw new RestClientException("累计回答超过长度限制");
                    }
                    answer.append(data.content());
                    onDelta.accept(data);
                }
                case "done" -> completed.set(
                        codec.readDone(event.data(), answer.toString()));
                case "error" -> {
                    var error = codec.readError(event.data());
                    throw new RestClientException(error.message());
                }
                default -> throw new RestClientException("未知推荐事件");
            }
        }, onOpen);

        checkCancelled();
        if (completed.get() == null) {
            throw new RestClientException("推荐说明缺少完整结束事件");
        }
        return completed.get().answer();
    }

    private AiCandidateProduct toCandidate(Product product) {
        return new AiCandidateProduct(
                product.getId(),
                product.getTitle(),
                product.getBrand(),
                product.getPrice().setScale(2, RoundingMode.UNNECESSARY).toPlainString(),
                product.getDescription());
    }

    private void checkCancelled() {
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException("流式导购任务已取消");
        }
    }
}
