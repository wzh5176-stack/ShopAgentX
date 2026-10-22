package com.wwzyhy.shopagent.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wwzyhy.shopagent.dto.MessagePageResponse;
import com.wwzyhy.shopagent.dto.MessageResponse;
import com.wwzyhy.shopagent.entity.ChatMessage;
import com.wwzyhy.shopagent.mapper.ChatMessageMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.wwzyhy.shopagent.dto.AiConversationMessage;
import com.wwzyhy.shopagent.dto.ConversationContext;


@Service
public class ChatMessageService {

    private final ChatMessageMapper chatMessageMapper;
    private final ChatConversationService chatConversationService;
    private final RecommendationJsonCodec recommendationJsonCodec;

    public ChatMessageService(
            ChatMessageMapper chatMessageMapper,
            ChatConversationService chatConversationService,
            RecommendationJsonCodec recommendationJsonCodec) {

        this.chatMessageMapper = chatMessageMapper;
        this.chatConversationService = chatConversationService;
        this.recommendationJsonCodec = recommendationJsonCodec;
    }

    @Transactional(readOnly = true)
    public MessagePageResponse readHistory(
            Authentication authentication,
            Long conversationId,
            Long beforeId,
            int size) {

        chatConversationService.getOwned(authentication, conversationId);

        if (size < 1 || size > 50) {
            throw new IllegalArgumentException("每次消息数量必须在1到50之间");
        }

        if (beforeId != null && beforeId <= 0) {
            throw new IllegalArgumentException("消息游标必须为正整数");
        }

        QueryWrapper<ChatMessage> query = new QueryWrapper<>();
        query.eq("conversation_id", conversationId);
        if (beforeId != null) {
            query.lt("id", beforeId);
        }
        query.orderByDesc("id");

        Page<ChatMessage> pageRequest = new Page<>(1, size + 1, false);
        Page<ChatMessage> result =
                chatMessageMapper.selectPage(pageRequest, query);

        List<ChatMessage> records = new ArrayList<>(result.getRecords());
        boolean hasMore = records.size() > size;
        if (hasMore) {
            records.remove(records.size() - 1);
        }

        Collections.reverse(records);

        List<MessageResponse> items = new ArrayList<>();
        for (ChatMessage message : records) {
            items.add(new MessageResponse(
                    message.getId(),
                    message.getRole(),
                    message.getContent(),
                    message.getCreatedAt(),
                    recommendationJsonCodec.fromJson(
                            message.getRecommendationJson()),
                    message.getMessageType(),
                    message.getCartOperationId()
            ));
        }

        Long nextBeforeId = hasMore ? records.get(0).getId() : null;

        return new MessagePageResponse(items, nextBeforeId, hasMore);
    }

    @Transactional(readOnly = true)
    public ConversationContext loadContext(
            Authentication authentication,
            Long conversationId) {

        chatConversationService.getOwned(authentication, conversationId);

        QueryWrapper<ChatMessage> query = new QueryWrapper<>();
        query.select("id", "role", "content");
        query.eq("conversation_id", conversationId);
        query.orderByDesc("id");

        Page<ChatMessage> pageRequest = new Page<>(1, 10, false);
        Page<ChatMessage> result =
                chatMessageMapper.selectPage(pageRequest, query);

        List<ChatMessage> records = new ArrayList<>(result.getRecords());

        long lastMessageId = records.isEmpty()
                ? 0L
                : records.get(0).getId();

        Collections.reverse(records);

        List<AiConversationMessage> history = new ArrayList<>();
        for (ChatMessage message : records) {
            history.add(new AiConversationMessage(
                    message.getRole(),
                    message.getContent()
            ));
        }

        return new ConversationContext(
                List.copyOf(history),
                lastMessageId
        );
    }
}