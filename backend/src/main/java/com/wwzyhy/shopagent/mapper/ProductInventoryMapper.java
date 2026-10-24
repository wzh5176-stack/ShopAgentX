package com.wwzyhy.shopagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wwzyhy.shopagent.entity.ProductInventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductInventoryMapper
        extends BaseMapper<ProductInventory> {

    @Update("""
            UPDATE product_inventory
            SET reserved_quantity = reserved_quantity - #{quantity}
            WHERE product_id = #{productId}
              AND #{quantity} BETWEEN 1 AND 99
              AND reserved_quantity >= #{quantity}
            """)
    int consume(
            @Param("productId") Long productId,
            @Param("quantity") Integer quantity);

    @Update("""
            UPDATE product_inventory
            SET available_quantity = available_quantity - #{quantity},
                reserved_quantity = reserved_quantity + #{quantity}
            WHERE product_id = #{productId}
              AND #{quantity} BETWEEN 1 AND 99
              AND available_quantity >= #{quantity}
              AND reserved_quantity <= 2147483647 - #{quantity}
            """)
    int reserve(
            @Param("productId") Long productId,
            @Param("quantity") Integer quantity);

    @Update("""
            UPDATE product_inventory
            SET available_quantity = available_quantity + #{quantity},
                reserved_quantity = reserved_quantity - #{quantity}
            WHERE product_id = #{productId}
              AND #{quantity} BETWEEN 1 AND 99
              AND reserved_quantity >= #{quantity}
              AND available_quantity <= 2147483647 - #{quantity}
            """)
    int release(
            @Param("productId") Long productId,
            @Param("quantity") Integer quantity);
}
