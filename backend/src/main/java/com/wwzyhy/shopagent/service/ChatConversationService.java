package com.wwzyhy.shopagent.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wwzyhy.shopagent.dto.ConversationCreateRequest;
import com.wwzyhy.shopagent.dto.ConversationResponse;
import com.wwzyhy.shopagent.entity.ChatConversation;
import com.wwzyhy.shopagent.mapper.ChatConversationMapper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.wwzyhy.shopagent.exception.ConversationNotFoundException;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wwzyhy.shopagent.dto.ConversationPageResponse;
import java.util.ArrayList;
import java.util.List;


@Service
public class ChatConversationService {

    private final ChatConversationMapper chatConversationMapper;
    private final CurrentUserService currentUserService;

    public ChatConversationService(
            ChatConversationMapper chatConversationMapper,
            CurrentUserService currentUserService) {
        this.chatConversationMapper = chatConversationMapper;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public ConversationResponse create(
            Authentication authentication,
            ConversationCreateRequest request) {

        Long userId = currentUserService.requireUserId(authentication);

        if (request == null
                || request.title() == null
                || request.title().isBlank()
                || request.title().length() > 100) {
            throw new IllegalArgumentException("会话标题不符合要求");
        }

        String title = request.title().strip();

        ChatConversation conversation = new ChatConversation();
        conversation.setUserId(userId);
        conversation.setTitle(title);

        int rows = chatConversationMapper.insert(conversation);

        if (rows != 1 || conversation.getId() == null) {
            throw new IllegalStateException("创建会话失败");
        }

        QueryWrapper<ChatConversation> query = new QueryWrapper<>();
        query.eq("id", conversation.getId());
        query.eq("user_id", userId);

        ChatConversation saved = chatConversationMapper.selectOne(query);

        if (saved == null
                || saved.getCreatedAt() == null
                || saved.getUpdatedAt() == null) {
            throw new IllegalStateException("读取新会话失败");
        }

        return new ConversationResponse(
                saved.getId(),
                saved.getTitle(),
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public ConversationResponse getOwned(
            Authentication authentication,
            Long id) {

        Long userId = currentUserService.requireUserId(authentication);

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("会话编号必须为正整数");
        }

        QueryWrapper<ChatConversation> query = new QueryWrapper<>();
        query.eq("id", id);
        query.eq("user_id", userId);

        ChatConversation conversation = chatConversationMapper.selectOne(query);

        if (conversation == null) {
            throw new ConversationNotFoundException();
        }

        return new ConversationResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }


    @Transactional(readOnly = true)
    public ConversationPageResponse listOwned(
            Authentication authentication,
            int page,
            int size) {

        Long userId = currentUserService.requireUserId(authentication);

        if (page < 1 || size < 1 || size > 50) {
            throw new IllegalArgumentException("分页参数不符合要求");
        }

        QueryWrapper<ChatConversation> query = new QueryWrapper<>();
        query.eq("user_id", userId);
        query.orderByDesc("updated_at");
        query.orderByDesc("id");

        Page<ChatConversation> pageRequest = new Page<>(page, size);
        Page<ChatConversation> result =
                chatConversationMapper.selectPage(pageRequest, query);

        List<ConversationResponse> items = new ArrayList<>();
        for (ChatConversation conversation : result.getRecords()) {
            items.add(new ConversationResponse(
                    conversation.getId(),
                    conversation.getTitle(),
                    conversation.getCreatedAt(),
                    conversation.getUpdatedAt()
            ));
        }

        return new ConversationPageResponse(
                items,
                result.getCurrent(),
                result.getSize(),
                result.getTotal(),
                result.getPages()
        );
    }
}