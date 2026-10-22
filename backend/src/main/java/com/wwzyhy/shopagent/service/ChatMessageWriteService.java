package com.wwzyhy.shopagent.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.wwzyhy.shopagent.dto.AssistantResponse;
import com.wwzyhy.shopagent.dto.MessageResponse;
import com.wwzyhy.shopagent.dto.MessageSendResponse;
import com.wwzyhy.shopagent.entity.ChatConversation;
import com.wwzyhy.shopagent.entity.ChatMessage;
import com.wwzyhy.shopagent.exception.ConversationNotFoundException;
import com.wwzyhy.shopagent.mapper.ChatConversationMapper;
import com.wwzyhy.shopagent.mapper.ChatMessageMapper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.wwzyhy.shopagent.exception.ConversationChangedException;


@Service
public class ChatMessageWriteService {

    private final CurrentUserService currentUserService;
    private final ChatConversationMapper chatConversationMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final RecommendationJsonCodec recommendationJsonCodec;

    public ChatMessageWriteService(
            CurrentUserService currentUserService,
            ChatConversationMapper chatConversationMapper,
            ChatMessageMapper chatMessageMapper,
            RecommendationJsonCodec recommendationJsonCodec) {
        this.currentUserService = currentUserService;
        this.chatConversationMapper = chatConversationMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.recommendationJsonCodec = recommendationJsonCodec;
    }

    @Transactional
    public MessageSendResponse saveTurn(
            Authentication authentication,
            Long conversationId,
            String content,
            AssistantResponse recommendation,
            long expectedLastMessageId) {

        Long userId = currentUserService.requireUserId(authentication);

        if (conversationId == null || conversationId <= 0) {
            throw new IllegalArgumentException("会话编号必须为正整数");
        }

        if (expectedLastMessageId < 0) {
            throw new IllegalArgumentException("历史消息编号不符合要求");
        }

        if (content == null || content.isBlank() || content.length() > 1000) {
            throw new IllegalArgumentException(
                    "消息必须为1到1000个字符且不能全为空白");
        }

        if (recommendation == null
                || recommendation.keyword() == null
                || recommendation.answer() == null
                || recommendation.answer().isBlank()
                || recommendation.products() == null) {
            throw new IllegalStateException("导购结果不完整");
        }

        String recommendationJson =
                recommendationJsonCodec.toJson(recommendation);

        QueryWrapper<ChatConversation> query = new QueryWrapper<>();
        query.eq("id", conversationId);
        query.eq("user_id", userId);
        query.last("FOR UPDATE");

        ChatConversation conversation =
                chatConversationMapper.selectOne(query);

        if (conversation == null) {
            throw new ConversationNotFoundException();
        }

        QueryWrapper<ChatMessage> latestQuery = new QueryWrapper<>();
        latestQuery.select("id");
        latestQuery.eq("conversation_id", conversationId);
        latestQuery.orderByDesc("id");
        latestQuery.last("LIMIT 1 FOR SHARE");

        ChatMessage latest = chatMessageMapper.selectOne(latestQuery);
        long actualLastMessageId = latest == null ? 0L : latest.getId();

        if (actualLastMessageId != expectedLastMessageId) {
            throw new ConversationChangedException();
        }


        ChatMessage userMessage =
                insertMessage(conversationId, "user", content, null);

        ChatMessage assistantMessage =
                insertMessage(
                        conversationId,
                        "assistant",
                        recommendation.answer(),
                        recommendationJson
                );

        UpdateWrapper<ChatConversation> update = new UpdateWrapper<>();
        update.eq("id", conversationId);
        update.eq("user_id", userId);
        update.setSql("updated_at = CURRENT_TIMESTAMP");
        chatConversationMapper.update(null, update);

        return new MessageSendResponse(
                new MessageResponse(
                        userMessage.getId(),
                        userMessage.getRole(),
                        userMessage.getContent(),
                        userMessage.getCreatedAt(),
                        null,
                        userMessage.getMessageType(),
                        userMessage.getCartOperationId()
                ),
                new MessageResponse(
                        assistantMessage.getId(),
                        assistantMessage.getRole(),
                        assistantMessage.getContent(),
                        assistantMessage.getCreatedAt(),
                        recommendation,
                        assistantMessage.getMessageType(),
                        assistantMessage.getCartOperationId()
                ),
                recommendation
        );
    }

    private ChatMessage insertMessage(
            Long conversationId,
            String role,
            String content,
            String recommendationJson) {

        ChatMessage message = new ChatMessage();
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content);
        message.setRecommendationJson(recommendationJson);

        int rows = chatMessageMapper.insert(message);

        if (rows != 1 || message.getId() == null) {
            throw new IllegalStateException("保存消息失败");
        }

        ChatMessage saved = chatMessageMapper.selectById(message.getId());

        if (saved == null || saved.getCreatedAt() == null) {
            throw new IllegalStateException("读取新消息失败");
        }

        return saved;
    }
}