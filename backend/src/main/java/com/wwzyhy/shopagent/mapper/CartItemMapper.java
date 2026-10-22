package com.wwzyhy.shopagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wwzyhy.shopagent.entity.CartItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

// MyBatis通过扫描创建此接口的代理，供Service注入调用。
@SuppressWarnings("unused")
@Mapper
public interface CartItemMapper extends BaseMapper<CartItem> {

    @Insert("""
            INSERT INTO cart_items (user_id, product_id, quantity)
            VALUES (#{userId}, #{productId}, #{quantity})
            ON DUPLICATE KEY UPDATE quantity = #{quantity}
            """)
    int setQuantity(
            @Param("userId") Long userId,
            @Param("productId") Long productId,
            @Param("quantity") Integer quantity);
}