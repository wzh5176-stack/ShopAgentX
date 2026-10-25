package com.wwzyhy.shopagent.service;

import com.wwzyhy.shopagent.dto.AiHealthData;
import com.wwzyhy.shopagent.dto.AiHealthResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import com.wwzyhy.shopagent.dto.AiEchoData;
import com.wwzyhy.shopagent.dto.AiEchoResponse;
import java.util.Map;
import com.wwzyhy.shopagent.dto.AiChatData;
import com.wwzyhy.shopagent.dto.AiChatResponse;
import com.wwzyhy.shopagent.dto.AiIntentData;
import com.wwzyhy.shopagent.dto.AiIntentResponse;
import com.wwzyhy.shopagent.dto.AiAskRequest;
import com.wwzyhy.shopagent.dto.AiAskResponse;
import com.wwzyhy.shopagent.dto.AiCandidateProduct;
import com.wwzyhy.shopagent.dto.AiRecommendationData;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.wwzyhy.shopagent.dto.AiKnowledgeData;
import com.wwzyhy.shopagent.dto.AiKnowledgeResponse;
import com.wwzyhy.shopagent.dto.AiKnowledgeSource;
import com.wwzyhy.shopagent.dto.AiConversationMessage;
import com.wwzyhy.shopagent.dto.AiConversationIntentRequest;
import com.wwzyhy.shopagent.dto.AiConversationIntentData;
import com.wwzyhy.shopagent.dto.AiConversationIntentResponse;
import com.wwzyhy.shopagent.dto.AiStreamEvent;
import com.wwzyhy.shopagent.client.AiSseReader;
import org.springframework.http.MediaType;
import java.util.function.Consumer;
import java.io.InputStream;import com.wwzyhy.shopagent.dto.AiProductSelectionData;
import com.wwzyhy.shopagent.dto.AiProductSelectionResponse;import com.wwzyhy.shopagent.dto.AiCartToolProduct;
import com.wwzyhy.shopagent.dto.AiCartToolIntentRequest;
import com.wwzyhy.shopagent.dto.AiCartToolIntentData;
import com.wwzyhy.shopagent.dto.AiCartToolIntentResponse;

@Service
public class AiClient {

    private final RestClient client;

    public AiClient(@Qualifier("aiRestClient") RestClient client) {
        this.client = client;
    }

    public AiHealthData health() {
        AiHealthResponse response = client.get()
                .uri("/health")
                .retrieve()
                .body(AiHealthResponse.class);

        if (response == null || response.data() == null
                || !"UP".equals(response.data().status())
                || !"shopagent-ai".equals(response.data().service())) {
            throw new RestClientException("Python健康响应不符合约定");
        }

        return response.data();
    }

    public AiEchoData echo(String message) {
        AiEchoResponse response = client.post()
                .uri("/api/echo")
                .body(Map.of("message", message))
                .retrieve()
                .body(AiEchoResponse.class);

        if (response == null || response.data() == null
                || !message.equals(response.data().message())
                || !"shopagent-ai".equals(response.data().service())) {
            throw new RestClientException("Python回声响应不符合约定");
        }

        return response.data();
    }

    public AiChatData chat(String message) {
        AiChatResponse response = client.post()
                .uri("/api/model/chat")
                .body(Map.of("message", message))
                .retrieve()
                .body(AiChatResponse.class);

        if (response == null || response.data() == null) {
            throw new RestClientException("Python模型响应缺少data");
        }

        AiChatData data = response.data();
        if (data.answer() == null || data.answer().isBlank()
                || !"shopagent-ai".equals(data.service())) {
            throw new RestClientException("Python模型响应不符合约定");
        }

        return data;
    }

    public AiIntentData parseIntent(String question) {
        AiIntentResponse response = client.post()
                .uri("/api/intent")
                .body(Map.of("question", question))
                .retrieve()
                .body(AiIntentResponse.class);

        if (response == null || response.data() == null) {
            throw new RestClientException("Python需求响应缺少data");
        }

        AiIntentData data = response.data();
        if (data.keyword() == null || data.keyword().length() > 50) {
            throw new RestClientException("Python返回的关键词不符合约定");
        }

        String maxPrice = data.maxPrice();
        if (maxPrice != null
                && !maxPrice.matches("(0|[1-9][0-9]{0,7})\\.[0-9]{2}")) {
            throw new RestClientException("Python返回的预算金额不符合约定");
        }

        return new AiIntentData(data.keyword().trim(), maxPrice);
    }


    public AiRecommendationData recommend(AiAskRequest request) {
        return recommend(request, null);
    }

    public AiRecommendationData recommend(
            AiAskRequest request,
            AiIntentData searchIntent) {

        Object requestBody = request;

        if (searchIntent != null) {
            requestBody = Map.of(
                    "question", request.question(),
                    "products", request.products(),
                    "searchIntent", searchIntent
            );
        }

        AiAskResponse response = client.post()
                .uri("/api/ask")
                .body(requestBody)
                .retrieve()
                .body(AiAskResponse.class);

        if (response == null || response.data() == null) {
            throw new RestClientException("Python推荐响应缺少data");
        }

        AiRecommendationData data = response.data();

        if (data.answer() == null || data.answer().isBlank()
                || data.answer().length() > 2000
                || data.recommendedIds() == null
                || data.recommendedIds().size() > 5) {
            throw new RestClientException("Python推荐响应不符合约定");
        }

        Set<Long> candidateIds = new HashSet<>();
        for (AiCandidateProduct product : request.products()) {
            candidateIds.add(product.id());
        }

        List<Long> recommendedIds = new ArrayList<>();
        for (Long id : data.recommendedIds()) {
            if (id == null || !candidateIds.contains(id)) {
                throw new RestClientException("Python推荐了候选列表之外的商品");
            }

            if (!recommendedIds.contains(id)) {
                recommendedIds.add(id);
            }
        }

        return new AiRecommendationData(
                data.answer().trim(),
                recommendedIds
        );
    }


    public AiKnowledgeData askKnowledge(String question) {
        return askKnowledge(question, null, null);
    }

    public AiKnowledgeData askKnowledge(String question, String productTitle, String productBrand) {
        Object body = productTitle == null ? Map.of("question", question) : Map.of(
                "question", question, "productTitle", productTitle, "productBrand", productBrand);
        AiKnowledgeResponse response = client.post()
                .uri("/api/knowledge/ask")
                .body(body)
                .retrieve()
                .body(AiKnowledgeResponse.class);

        if (response == null || response.data() == null) {
            throw new RestClientException("Python知识响应缺少data");
        }

        AiKnowledgeData data = response.data();
        if (!isKnowledgeText(data.answer(), 2000)
                || data.answerable() == null
                || data.sources() == null
                || data.sources().size() > 3) {
            throw new RestClientException("Python知识响应不符合约定");
        }

        if (data.answerable() && data.sources().isEmpty()) {
            throw new RestClientException("有依据的知识回答缺少引用");
        }
        if (!data.answerable() && !data.sources().isEmpty()) {
            throw new RestClientException("资料不足的回答不应附带引用");
        }

        Set<String> chunkIds = new HashSet<>();
        for (AiKnowledgeSource source : data.sources()) {
            if (source == null
                    || !isKnowledgeText(source.chunkId(), 200)
                    || !isKnowledgeText(source.source(), 200)
                    || !isKnowledgeText(source.document(), 2000)
                    || !isKnowledgeText(source.section(), 200)
                    || !isKnowledgeText(source.content(), 5000)) {
                throw new RestClientException("Python返回的知识引用不完整");
            }

            if (!chunkIds.add(source.chunkId())) {
                throw new RestClientException("Python返回了重复的知识引用");
            }
            if (productTitle != null && !source.document().startsWith(productTitle + " · 资料版本 ")) {
                throw new RestClientException("Python资料引用与所选商品不一致");
            }
        }

        return new AiKnowledgeData(
                data.answer().trim(),
                data.answerable(),
                List.copyOf(data.sources())
        );
    }

    private boolean isKnowledgeText(String value, int maxLength) {
        return value != null
                && !value.isBlank()
                && value.length() <= maxLength;
    }


    public AiConversationIntentData parseConversationIntent(
            AiConversationIntentRequest request) {

        if (request == null
                || request.question() == null
                || request.question().isBlank()
                || request.question().length() > 1000
                || request.history() == null
                || request.history().size() > 10) {
            throw new IllegalArgumentException("对话需求请求不符合约定");
        }

        for (AiConversationMessage message : request.history()) {
            if (message == null
                    || !("user".equals(message.role())
                    || "assistant".equals(message.role()))
                    || message.content() == null
                    || message.content().isBlank()
                    || message.content().length() > 2000) {
                throw new IllegalArgumentException("历史消息不符合约定");
            }
        }

        AiConversationIntentResponse response = client.post()
                .uri("/api/conversations/intent")
                .body(request)
                .retrieve()
                .body(AiConversationIntentResponse.class);

        if (response == null || response.data() == null) {
            throw new RestClientException("Python对话需求响应缺少data");
        }

        AiConversationIntentData data = response.data();

        if (data.keyword() == null || data.keyword().length() > 50) {
            throw new RestClientException("Python返回的关键词不符合约定");
        }
        String keyword = data.keyword().strip();

        String maxPrice = data.maxPrice();
        if (maxPrice != null
                && !maxPrice.matches("(0|[1-9][0-9]{0,7})\\.[0-9]{2}")) {
            throw new RestClientException("Python返回的预算金额不符合约定");
        }

        String clarification = data.clarification();
        if (clarification != null) {
            if (clarification.isBlank() || clarification.length() > 200) {
                throw new RestClientException("Python返回的澄清问题不符合约定");
            }
            clarification = clarification.strip();
        }

        if (keyword.isEmpty() && clarification == null) {
            throw new RestClientException("Python未提供商品品类或澄清问题");
        }

        return new AiConversationIntentData(
                keyword,
                maxPrice,
                clarification
        );
    }

    public AiProductSelectionData selectProducts(
            AiAskRequest request, AiIntentData searchIntent) {

        Set<Long> candidateIds = validateRecommendationInput(request, searchIntent, 20);

        AiProductSelectionResponse response = client.post()
                .uri("/api/recommendations/select")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "question", request.question().strip(),
                        "products", request.products(),
                        "searchIntent", searchIntent))
                .retrieve()
                .body(AiProductSelectionResponse.class);

        if (response == null || response.data() == null
                || response.data().recommendedIds() == null
                || response.data().recommendedIds().size() > 5) {
            throw new RestClientException("Python商品选择响应不符合约定");
        }

        List<Long> selectedIds = new ArrayList<>();
        for (Long id : response.data().recommendedIds()) {
            if (id == null || !candidateIds.contains(id)) {
                throw new RestClientException("Python选择了候选列表之外的商品");
            }
            if (!selectedIds.contains(id)) {
                selectedIds.add(id);
            }
        }
        return new AiProductSelectionData(List.copyOf(selectedIds));
    }

    public void streamRecommendation(
            AiAskRequest request,
            AiIntentData searchIntent,
            Consumer<AiStreamEvent> onEvent,
            Consumer<InputStream> onOpen) {

        validateRecommendationInput(request, searchIntent, 5);
        streamRequest(
                "/api/recommendations/explain/stream",
                Map.of(
                        "question", request.question().strip(),
                        "products", request.products(),
                        "searchIntent", searchIntent),
                onEvent,
                onOpen);
    }

    public void streamChat(String message, Consumer<AiStreamEvent> onEvent) {
        streamChat(message, onEvent, input -> { });
    }

    public void streamChat(
            String message,
            Consumer<AiStreamEvent> onEvent,
            Consumer<InputStream> onOpen) {

        if (message == null || message.isBlank() || message.length() > 1000) {
            throw new IllegalArgumentException("消息必须为1到1000个字符且不能全为空白");
        }
        streamRequest(
                "/api/model/chat/stream",
                Map.of("message", message.strip()),
                onEvent,
                onOpen);
    }

    private Set<Long> validateRecommendationInput(
            AiAskRequest request, AiIntentData searchIntent, int maxProducts) {

        if (request == null || request.question() == null
                || request.question().isBlank() || request.question().length() > 1000
                || request.products() == null || request.products().isEmpty()
                || request.products().size() > maxProducts) {
            throw new IllegalArgumentException("商品推荐请求不符合约定");
        }
        if (searchIntent == null || searchIntent.keyword() == null
                || searchIntent.keyword().isBlank() || searchIntent.keyword().length() > 50
                || (searchIntent.maxPrice() != null
                && !searchIntent.maxPrice().matches("(0|[1-9][0-9]{0,7})\\.[0-9]{2}"))) {
            throw new IllegalArgumentException("合并后的商品条件不符合约定");
        }

        Set<Long> ids = new HashSet<>();
        for (AiCandidateProduct product : request.products()) {
            if (product == null || product.id() == null || product.id() <= 0
                    || !ids.add(product.id())) {
                throw new IllegalArgumentException("候选商品编号必须为正数且不能重复");
            }
        }
        return ids;
    }

    private void streamRequest(
            String path,
            Object body,
            Consumer<AiStreamEvent> onEvent,
            Consumer<InputStream> onOpen) {

        if (onEvent == null || onOpen == null) {
            throw new IllegalArgumentException("必须提供事件和连接处理方法");
        }

        client.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .body(body)
                .exchange((request, response) -> {
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        throw new RestClientException("Python流式接口请求失败");
                    }

                    MediaType contentType;
                    try {
                        contentType = response.getHeaders().getContentType();
                    } catch (IllegalArgumentException ex) {
                        throw new RestClientException("Python响应类型格式错误", ex);
                    }

                    if (contentType == null
                            || !MediaType.TEXT_EVENT_STREAM.isCompatibleWith(contentType)) {
                        throw new RestClientException("Python没有返回事件流");
                    }

                    InputStream input = response.getBody();
                    onOpen.accept(input);
                    new AiSseReader().read(input, onEvent);
                    return null;
                });
    }
    public AiCartToolIntentData parseCartToolIntent(
            AiCartToolIntentRequest request) {

        if (request == null
                || request.question() == null
                || request.question().isBlank()
                || request.question().length() > 1000
                || request.products() == null
                || request.products().isEmpty()
                || request.products().size() > 5) {
            throw new IllegalArgumentException("购物车意图请求不符合要求");
        }

        for (AiCartToolProduct product : request.products()) {
            if (product == null
                    || product.title() == null || product.title().isBlank()
                    || product.title().length() > 200
                    || product.brand() == null || product.brand().isBlank()
                    || product.brand().length() > 100
                    || product.price() == null
                    || !product.price().matches(
                    "(0|[1-9][0-9]{0,7})\\.[0-9]{2}")) {
                throw new IllegalArgumentException("购物车意图商品不符合要求");
            }
        }

        AiCartToolIntentRequest body = new AiCartToolIntentRequest(
                request.question().strip(),
                List.copyOf(request.products()));

        AiCartToolIntentResponse response = client.post()
                .uri("/api/cart/intent")
                .body(body)
                .retrieve()
                .body(AiCartToolIntentResponse.class);

        if (response == null || response.data() == null) {
            throw new RestClientException("Python购物车意图响应缺少data");
        }

        AiCartToolIntentData data = response.data();

        if ("set_quantity".equals(data.action())) {
            if (data.productIndex() == null
                    || data.productIndex() < 1
                    || data.productIndex() > body.products().size()
                    || data.quantity() == null
                    || data.quantity() < 1
                    || data.quantity() > 99) {
                throw new RestClientException("Python购物车操作参数不符合约定");
            }
        } else if ("clarify".equals(data.action())
                || "none".equals(data.action())) {
            if (data.productIndex() != null || data.quantity() != null) {
                throw new RestClientException("Python非执行意图携带了操作参数");
            }
        } else {
            throw new RestClientException("Python购物车操作类型不符合约定");
        }

        return data;
    }
}
