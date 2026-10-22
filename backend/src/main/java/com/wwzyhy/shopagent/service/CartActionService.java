package com.wwzyhy.shopagent.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wwzyhy.shopagent.dto.CartActionCreateRequest;
import com.wwzyhy.shopagent.dto.CartActionResponse;
import com.wwzyhy.shopagent.dto.CartActionStatus;
import com.wwzyhy.shopagent.dto.CartItemSetResponse;
import com.wwzyhy.shopagent.dto.ProductSummaryResponse;
import com.wwzyhy.shopagent.entity.CartAction;
import com.wwzyhy.shopagent.entity.CartItem;
import com.wwzyhy.shopagent.entity.Product;
import com.wwzyhy.shopagent.exception.CartActionNotFoundException;
import com.wwzyhy.shopagent.exception.CartProductUnavailableException;
import com.wwzyhy.shopagent.mapper.CartActionMapper;
import com.wwzyhy.shopagent.mapper.CartItemMapper;
import com.wwzyhy.shopagent.mapper.ProductMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;

@Service
public class CartActionService {

    private final CartService cartService;
    private final CurrentUserService currentUserService;
    private final ChatConversationService chatConversationService;
    private final RecommendationSourceService recommendationSourceService;
    private final ProductMapper productMapper;
    private final CartItemMapper cartItemMapper;
    private final CartActionMapper cartActionMapper;
    private final Clock clock;

    public CartActionService(
            CurrentUserService currentUserService,
            ChatConversationService chatConversationService,
            RecommendationSourceService recommendationSourceService,
            ProductMapper productMapper,
            CartItemMapper cartItemMapper,
            CartActionMapper cartActionMapper,
            Clock clock,
            CartService cartService) {

        this.currentUserService = currentUserService;
        this.chatConversationService = chatConversationService;
        this.recommendationSourceService = recommendationSourceService;
        this.productMapper = productMapper;
        this.cartItemMapper = cartItemMapper;
        this.cartActionMapper = cartActionMapper;
        this.clock = clock;
        this.cartService = cartService;
    }

    @Transactional
    public CartActionResponse create(
            Authentication authentication,
            Long conversationId,
            CartActionCreateRequest request) {

        Long userId = currentUserService.requireUserId(authentication);

        if (request == null
                || request.sourceMessageId() == null
                || request.sourceMessageId() <= 0
                || request.productId() == null
                || request.productId() <= 0
                || request.quantity() == null
                || request.quantity() < 1
                || request.quantity() > 99) {
            throw new IllegalArgumentException("购物车操作参数不符合要求");
        }

        List<ProductSummaryResponse> products =
                recommendationSourceService.loadProducts(
                        authentication,
                        conversationId,
                        request.sourceMessageId());

        boolean recommended = false;
        for (ProductSummaryResponse product : products) {
            if (request.productId().equals(product.id())) {
                recommended = true;
                break;
            }
        }

        if (!recommended) {
            throw new IllegalArgumentException("商品不在指定消息的推荐列表中");
        }

        QueryWrapper<Product> productQuery = new QueryWrapper<>();
        productQuery.eq("id", request.productId());
        productQuery.eq("status", 1);
        productQuery.last("FOR SHARE");

        Product product = productMapper.selectOne(productQuery);
        if (product == null) {
            throw new CartProductUnavailableException();
        }

        QueryWrapper<CartItem> itemQuery = new QueryWrapper<>();
        itemQuery.eq("user_id", userId);
        itemQuery.eq("product_id", request.productId());
        itemQuery.last("FOR SHARE");

        CartItem item = cartItemMapper.selectOne(itemQuery);
        int baseQuantity = item == null ? 0 : item.getQuantity();

        LocalDateTime now = LocalDateTime.now(clock);

        CartAction action = new CartAction();
        action.setOperationId(UUID.randomUUID().toString());
        action.setUserId(userId);
        action.setConversationId(conversationId);
        action.setSourceMessageId(request.sourceMessageId());
        action.setProductId(product.getId());
        action.setTitle(product.getTitle());
        action.setBaseQuantity(baseQuantity);
        action.setQuantity(request.quantity());
        action.setStatus(CartActionStatus.PENDING.name());
        action.setExpiresAt(now.plusMinutes(10));
        action.setExecutedQuantity(null);
        action.setCreatedAt(now);
        action.setUpdatedAt(now);

        int rows = cartActionMapper.insert(action);
        if (rows != 1) {
            throw new IllegalStateException("创建购物车操作失败");
        }

        CartAction saved = findOwned(
                userId,
                conversationId,
                action.getOperationId());

        if (saved == null) {
            throw new IllegalStateException("读取新建购物车操作失败");
        }

        return toResponse(saved, LocalDateTime.now(clock));
    }

    @Transactional(readOnly = true)
    public CartActionResponse getOwned(
            Authentication authentication,
            Long conversationId,
            String operationId) {

        Long userId = currentUserService.requireUserId(authentication);
        chatConversationService.getOwned(authentication, conversationId);
        validateOperationId(operationId);

        CartAction action = findOwned(userId, conversationId, operationId);
        if (action == null) {
            throw new CartActionNotFoundException();
        }

        return toResponse(action, LocalDateTime.now(clock));
    }

    private CartAction findOwned(
            Long userId,
            Long conversationId,
            String operationId) {

        QueryWrapper<CartAction> query = new QueryWrapper<>();
        query.eq("operation_id", operationId);
        query.eq("user_id", userId);
        query.eq("conversation_id", conversationId);

        return cartActionMapper.selectOne(query);
    }

    private CartActionResponse toResponse(
            CartAction action,
            LocalDateTime now) {

        CartActionStatus status = CartActionStatus.valueOf(action.getStatus());

        if (status == CartActionStatus.PENDING
                && !action.getExpiresAt().isAfter(now)) {
            status = CartActionStatus.EXPIRED;
        }

        CartItemSetResponse result = null;
        if (status == CartActionStatus.SUCCEEDED) {
            if (action.getExecutedQuantity() == null
                    || !action.getExecutedQuantity().equals(action.getQuantity())) {
                throw new IllegalStateException("已执行操作缺少有效结果");
            }

            result = new CartItemSetResponse(
                    action.getProductId(),
                    action.getExecutedQuantity());
        }

        return new CartActionResponse(
                action.getOperationId(),
                action.getConversationId(),
                action.getSourceMessageId(),
                action.getProductId(),
                action.getTitle(),
                action.getBaseQuantity(),
                action.getQuantity(),
                status,
                action.getExpiresAt(),
                result);
    }

    private void validateOperationId(String operationId) {
        if (operationId == null
                || !operationId.matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-"
                        + "[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
            throw new IllegalArgumentException("操作编号格式不正确");
        }
    }

    @Transactional
    public CartActionResponse confirm(
            Authentication authentication,
            Long conversationId,
            String operationId) {

        Long userId = currentUserService.requireUserId(authentication);
        chatConversationService.getOwned(authentication, conversationId);
        validateOperationId(operationId);

        CartAction action = lockOwned(userId, conversationId, operationId);
        LocalDateTime now = LocalDateTime.now(clock);

        if (!CartActionStatus.PENDING.name().equals(action.getStatus())) {
            return toResponse(action, now);
        }

        if (!action.getExpiresAt().isAfter(now)) {
            return finishAction(action, CartActionStatus.EXPIRED, null, now);
        }

        QueryWrapper<Product> productQuery = new QueryWrapper<>();
        productQuery.eq("id", action.getProductId());
        productQuery.last("FOR UPDATE");

        Product product = productMapper.selectOne(productQuery);
        now = LocalDateTime.now(clock);

        if (!action.getExpiresAt().isAfter(now)) {
            return finishAction(action, CartActionStatus.EXPIRED, null, now);
        }

        if (product == null || !Integer.valueOf(1).equals(product.getStatus())) {
            return finishAction(action, CartActionStatus.REJECTED, null, now);
        }

        QueryWrapper<CartItem> itemQuery = new QueryWrapper<>();
        itemQuery.eq("user_id", userId);
        itemQuery.eq("product_id", action.getProductId());
        itemQuery.last("FOR UPDATE");

        CartItem current = cartItemMapper.selectOne(itemQuery);
        now = LocalDateTime.now(clock);

        if (!action.getExpiresAt().isAfter(now)) {
            return finishAction(action, CartActionStatus.EXPIRED, null, now);
        }

        int currentQuantity = current == null ? 0 : current.getQuantity();
        if (!Integer.valueOf(currentQuantity).equals(action.getBaseQuantity())) {
            return finishAction(action, CartActionStatus.CONFLICTED, null, now);
        }

        CartItem saved = cartService.setQuantity(
                authentication,
                action.getProductId(),
                action.getQuantity());

        return finishAction(
                action,
                CartActionStatus.SUCCEEDED,
                saved.getQuantity(),
                LocalDateTime.now(clock));
    }

    @Transactional
    public CartActionResponse cancel(
            Authentication authentication,
            Long conversationId,
            String operationId) {

        Long userId = currentUserService.requireUserId(authentication);
        chatConversationService.getOwned(authentication, conversationId);
        validateOperationId(operationId);

        CartAction action = lockOwned(userId, conversationId, operationId);
        LocalDateTime now = LocalDateTime.now(clock);

        if (!CartActionStatus.PENDING.name().equals(action.getStatus())) {
            return toResponse(action, now);
        }

        if (!action.getExpiresAt().isAfter(now)) {
            return finishAction(action, CartActionStatus.EXPIRED, null, now);
        }

        return finishAction(action, CartActionStatus.CANCELLED, null, now);
    }

    private CartAction lockOwned(
            Long userId,
            Long conversationId,
            String operationId) {

        QueryWrapper<CartAction> query = new QueryWrapper<>();
        query.eq("operation_id", operationId);
        query.eq("user_id", userId);
        query.eq("conversation_id", conversationId);
        query.last("FOR UPDATE");

        CartAction action = cartActionMapper.selectOne(query);
        if (action == null) {
            throw new CartActionNotFoundException();
        }

        return action;
    }

    private CartActionResponse finishAction(
            CartAction action,
            CartActionStatus status,
            Integer executedQuantity,
            LocalDateTime now) {

        UpdateWrapper<CartAction> update = new UpdateWrapper<>();
        update.eq("operation_id", action.getOperationId());
        update.eq("user_id", action.getUserId());
        update.eq("conversation_id", action.getConversationId());
        update.eq("status", CartActionStatus.PENDING.name());
        update.set("status", status.name());
        update.set("executed_quantity", executedQuantity);
        update.set("updated_at", now);

        int rows = cartActionMapper.update(null, update);
        if (rows != 1) {
            throw new IllegalStateException("保存购物车操作结果失败");
        }

        action.setStatus(status.name());
        action.setExecutedQuantity(executedQuantity);
        action.setUpdatedAt(now);

        return toResponse(action, now);
    }
}