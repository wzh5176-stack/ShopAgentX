package com.wwzyhy.shopagent.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wwzyhy.shopagent.entity.CartItem;
import com.wwzyhy.shopagent.entity.Product;
import com.wwzyhy.shopagent.exception.CartProductUnavailableException;
import com.wwzyhy.shopagent.mapper.CartItemMapper;
import com.wwzyhy.shopagent.mapper.ProductMapper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.wwzyhy.shopagent.dto.CartItemResponse;
import com.wwzyhy.shopagent.dto.CartResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class CartService {

    private final CurrentUserService currentUserService;
    private final ProductMapper productMapper;
    private final CartItemMapper cartItemMapper;

    public CartService(
            CurrentUserService currentUserService,
            ProductMapper productMapper,
            CartItemMapper cartItemMapper) {
        this.currentUserService = currentUserService;
        this.productMapper = productMapper;
        this.cartItemMapper = cartItemMapper;
    }

    @Transactional
    public CartItem setQuantity(
            Authentication authentication,
            Long productId,
            Integer quantity) {

        Long userId = currentUserService.requireUserId(authentication);
        validateProductId(productId);

        if (quantity == null || quantity < 1 || quantity > 99) {
            throw new IllegalArgumentException("商品数量必须为1到99");
        }

        QueryWrapper<Product> productQuery = new QueryWrapper<>();
        productQuery.eq("id", productId);
        productQuery.eq("status", 1);
        productQuery.last("FOR SHARE");

        Product product = productMapper.selectOne(productQuery);

        if (product == null) {
            throw new CartProductUnavailableException();
        }

        cartItemMapper.setQuantity(userId, productId, quantity);

        QueryWrapper<CartItem> itemQuery = new QueryWrapper<>();
        itemQuery.eq("user_id", userId);
        itemQuery.eq("product_id", productId);
        itemQuery.last("FOR SHARE");

        CartItem item = cartItemMapper.selectOne(itemQuery);

        if (item == null || item.getId() == null
                || !quantity.equals(item.getQuantity())) {
            throw new IllegalStateException("购物车保存结果不符合预期");
        }

        return item;
    }

    @Transactional
    public void remove(Authentication authentication, Long productId) {
        Long userId = currentUserService.requireUserId(authentication);
        validateProductId(productId);

        QueryWrapper<CartItem> query = new QueryWrapper<>();
        query.eq("user_id", userId);
        query.eq("product_id", productId);

        cartItemMapper.delete(query);
    }

    @Transactional(readOnly = true)
    public CartResponse list(Authentication authentication) {
        Long userId = currentUserService.requireUserId(authentication);

        QueryWrapper<CartItem> cartQuery = new QueryWrapper<>();
        cartQuery.eq("user_id", userId);
        cartQuery.orderByAsc("id");

        List<CartItem> cartItems = cartItemMapper.selectList(cartQuery);

        if (cartItems.isEmpty()) {
            return new CartResponse(List.of(), 0L, "0.00");
        }

        List<Long> productIds = new ArrayList<>();
        for (CartItem item : cartItems) {
            productIds.add(item.getProductId());
        }

        QueryWrapper<Product> productQuery = new QueryWrapper<>();
        productQuery.in("id", productIds);

        List<Product> products = productMapper.selectList(productQuery);

        Map<Long, Product> productsById = new HashMap<>();
        for (Product product : products) {
            productsById.put(product.getId(), product);
        }

        List<CartItemResponse> items = new ArrayList<>();
        long totalQuantity = 0L;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem item : cartItems) {
            totalQuantity += item.getQuantity();
            Product product = productsById.get(item.getProductId());

            if (product == null) {
                items.add(new CartItemResponse(
                        item.getProductId(),
                        "商品已删除或不存在",
                        null,
                        null,
                        null,
                        item.getQuantity(),
                        false,
                        "0.00"));
                continue;
            }

            boolean available = Integer.valueOf(1).equals(product.getStatus());

            BigDecimal subtotal = available
                    ? product.getPrice().multiply(
                    BigDecimal.valueOf(item.getQuantity()))
                    : BigDecimal.ZERO;

            totalAmount = totalAmount.add(subtotal);

            items.add(new CartItemResponse(
                    product.getId(),
                    product.getTitle(),
                    product.getBrand(),
                    money(product.getPrice()),
                    product.getImageUrl(),
                    item.getQuantity(),
                    available,
                    money(subtotal)));
        }

        return new CartResponse(
                List.copyOf(items),
                totalQuantity,
                money(totalAmount));
    }

    private String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }

    private void validateProductId(Long productId) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("商品编号必须为正整数");
        }
    }
}