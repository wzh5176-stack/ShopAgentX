package com.wwzyhy.shopagent.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wwzyhy.shopagent.dto.AssistantResponse;
import com.wwzyhy.shopagent.dto.ProductSummaryResponse;
import com.wwzyhy.shopagent.entity.ChatMessage;
import com.wwzyhy.shopagent.exception.RecommendationSourceNotFoundException;
import com.wwzyhy.shopagent.mapper.ChatMessageMapper;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecommendationSourceService {

    private final ChatConversationService chatConversationService;
    private final ChatMessageMapper chatMessageMapper;
    private final RecommendationJsonCodec recommendationJsonCodec;

    public RecommendationSourceService(
            ChatConversationService chatConversationService,
            ChatMessageMapper chatMessageMapper,
            RecommendationJsonCodec recommendationJsonCodec) {

        this.chatConversationService = chatConversationService;
        this.chatMessageMapper = chatMessageMapper;
        this.recommendationJsonCodec = recommendationJsonCodec;
    }

    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> loadProducts(
            Authentication authentication,
            Long conversationId,
            Long sourceMessageId) {

        chatConversationService.getOwned(authentication, conversationId);

        if (sourceMessageId == null || sourceMessageId <= 0) {
            throw new IllegalArgumentException("来源消息编号必须为正整数");
        }

        QueryWrapper<ChatMessage> query = new QueryWrapper<>();
        query.eq("id", sourceMessageId);
        query.eq("conversation_id", conversationId);
        query.eq("role", "assistant");

        ChatMessage source = chatMessageMapper.selectOne(query);
        if (source == null) {
            throw new RecommendationSourceNotFoundException();
        }

        AssistantResponse recommendation = recommendationJsonCodec.fromJson(
                source.getRecommendationJson());

        if (recommendation == null
                || recommendation.products() == null
                || recommendation.products().isEmpty()) {
            throw new IllegalArgumentException("这条消息没有可引用的推荐商品");
        }

        List<ProductSummaryResponse> products = recommendation.products();
        if (products.size() > 5) {
            throw new IllegalStateException("已保存的推荐商品数量不符合约定");
        }

        Set<Long> productIds = new HashSet<>();
        for (ProductSummaryResponse product : products) {
            if (product == null
                    || product.id() == null || product.id() <= 0
                    || !productIds.add(product.id())
                    || product.title() == null || product.title().isBlank()
                    || product.title().length() > 200
                    || product.brand() == null || product.brand().isBlank()
                    || product.brand().length() > 100
                    || product.price() == null
                    || !product.price().matches(
                    "(0|[1-9][0-9]{0,7})\\.[0-9]{2}")) {
                throw new IllegalStateException("已保存的推荐商品数据不符合约定");
            }
        }

        return List.copyOf(products);
    }
}