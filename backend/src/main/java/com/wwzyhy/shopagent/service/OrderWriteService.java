package com.wwzyhy.shopagent.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wwzyhy.shopagent.dto.CheckoutItemRequest;
import com.wwzyhy.shopagent.dto.OrderDetailResponse;
import com.wwzyhy.shopagent.dto.OrderStatus;
import com.wwzyhy.shopagent.dto.PreparedOrderRequest;
import com.wwzyhy.shopagent.entity.OrderItem;
import com.wwzyhy.shopagent.entity.Product;
import com.wwzyhy.shopagent.entity.ShopOrder;
import com.wwzyhy.shopagent.entity.UserAddress;
import com.wwzyhy.shopagent.exception.AddressNotFoundException;
import com.wwzyhy.shopagent.exception.CheckoutUnavailableException;
import com.wwzyhy.shopagent.mapper.OrderItemMapper;
import com.wwzyhy.shopagent.mapper.ProductInventoryMapper;
import com.wwzyhy.shopagent.mapper.ProductMapper;
import com.wwzyhy.shopagent.mapper.ShopOrderMapper;
import com.wwzyhy.shopagent.mapper.UserAddressMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderWriteService {

    private final ShopOrderMapper orderMapper;
    private final OrderItemMapper itemMapper;
    private final ProductMapper productMapper;
    private final UserAddressMapper addressMapper;
    private final ProductInventoryMapper inventoryMapper;
    private final Clock clock;

    public OrderWriteService(
            ShopOrderMapper orderMapper,
            OrderItemMapper itemMapper,
            ProductMapper productMapper,
            UserAddressMapper addressMapper,
            ProductInventoryMapper inventoryMapper,
            Clock clock) {
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
        this.productMapper = productMapper;
        this.addressMapper = addressMapper;
        this.inventoryMapper = inventoryMapper;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OrderDetailResponse create(
            Long userId,
            PreparedOrderRequest request) {
        UserAddress address = lockAddress(userId, request.addressId());

        List<OrderItem> rows = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CheckoutItemRequest item : request.items()) {
            Product product = lockProduct(item.productId());

            BigDecimal lineAmount = product.getPrice().multiply(
                    BigDecimal.valueOf(item.quantity()));

            OrderItem row = new OrderItem();
            row.setProductId(product.getId());
            row.setTitle(product.getTitle());
            row.setBrand(product.getBrand());
            row.setImageUrl(product.getImageUrl());
            row.setUnitPrice(product.getPrice());
            row.setQuantity(item.quantity());
            row.setLineAmount(lineAmount);

            rows.add(row);
            totalAmount = totalAmount.add(lineAmount);
        }

        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CheckoutUnavailableException(
                    "本轮暂不支持总金额为0的订单");
        }

        LocalDateTime now = LocalDateTime.now(clock)
                .truncatedTo(ChronoUnit.MILLIS);

        ShopOrder order = new ShopOrder();
        order.setUserId(userId);
        order.setRequestKey(request.requestKey());
        order.setRequestHash(request.requestHash());
        order.setStatus(OrderStatus.PENDING_PAYMENT.name());
        order.setTotalAmount(totalAmount);
        order.setRecipientName(address.getRecipientName());
        order.setPhone(address.getPhone());
        order.setProvince(address.getProvince());
        order.setCity(address.getCity());
        order.setDistrict(address.getDistrict());
        order.setDetailAddress(address.getDetailAddress());
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        order.setExpiresAt(now.plusMinutes(15));

        if (orderMapper.insert(order) != 1 || order.getId() == null) {
            throw new IllegalStateException("创建订单失败");
        }

        for (OrderItem row : rows) {
            int reserved = inventoryMapper.reserve(
                    row.getProductId(), row.getQuantity());

            if (reserved != 1) {
                throw new CheckoutUnavailableException(
                        "商品库存不足，请调整购买数量后重试");
            }

            row.setOrderId(order.getId());

            if (itemMapper.insert(row) != 1 || row.getId() == null) {
                throw new IllegalStateException("保存订单明细失败");
            }
        }

        if (!LocalDateTime.now(clock).isBefore(order.getExpiresAt())) {
            throw new CheckoutUnavailableException(
                    "创建订单耗时过长，请重试");
        }

        return readSaved(userId, order.getId(), rows.size());
    }

    private UserAddress lockAddress(Long userId, Long addressId) {
        QueryWrapper<UserAddress> query = new QueryWrapper<>();
        query.eq("id", addressId);
        query.eq("user_id", userId);
        query.last("FOR SHARE");

        UserAddress address = addressMapper.selectOne(query);

        if (address == null) {
            throw new AddressNotFoundException();
        }

        return address;
    }

    private Product lockProduct(Long productId) {
        QueryWrapper<Product> query = new QueryWrapper<>();
        query.eq("id", productId);
        query.eq("status", 1);
        query.last("FOR SHARE");

        Product product = productMapper.selectOne(query);

        if (product == null) {
            throw new CheckoutUnavailableException(
                    "部分商品不存在或已下架，请刷新后重选");
        }

        return product;
    }

    private OrderDetailResponse readSaved(
            Long userId,
            Long orderId,
            int expectedItems) {
        QueryWrapper<ShopOrder> orderQuery = new QueryWrapper<>();
        orderQuery.eq("id", orderId);
        orderQuery.eq("user_id", userId);
        orderQuery.last("FOR SHARE");

        ShopOrder order = orderMapper.selectOne(orderQuery);

        QueryWrapper<OrderItem> itemQuery = new QueryWrapper<>();
        itemQuery.eq("order_id", orderId);
        itemQuery.orderByAsc("id");
        itemQuery.last("FOR SHARE");

        List<OrderItem> rows = itemMapper.selectList(itemQuery);

        if (order == null || rows.size() != expectedItems) {
            throw new IllegalStateException("读取保存后的订单失败");
        }

        return OrderDetailResponse.from(order, rows);
    }
}