package com.wwzyhy.shopagent.service;

import com.wwzyhy.shopagent.dto.*;
import com.wwzyhy.shopagent.exception.InventoryConflictException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {
    private final JdbcTemplate jdbc;

    public InventoryService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static final RowMapper<InventoryReplenishmentResponse> ROW = (rs, row) -> read(rs);
    private static InventoryReplenishmentResponse read(ResultSet rs) throws SQLException {
        return new InventoryReplenishmentResponse(rs.getLong("id"), rs.getLong("product_id"),
                rs.getString("request_key"), rs.getInt("quantity"), rs.getString("reason"),
                rs.getString("actor"), rs.getInt("available_quantity"), rs.getInt("reserved_quantity"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }

    public Map<Long, Integer> availableFor(List<Long> productIds) {
        if (productIds.isEmpty()) return Map.of();
        List<Long> ids = productIds.stream().distinct().toList();
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        Map<Long, Integer> result = new HashMap<>();
        jdbc.query("SELECT product_id,available_quantity FROM product_inventory WHERE product_id IN (" + placeholders + ")",
                ps -> { for (int i = 0; i < ids.size(); i++) ps.setLong(i + 1, ids.get(i)); },
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> result.put(rs.getLong(1), rs.getInt(2)));
        return result;
    }

    public InventoryResponse get(Long productId) {
        validId(productId);
        var rows = jdbc.query("SELECT product_id,available_quantity,reserved_quantity FROM product_inventory WHERE product_id=?",
                (rs, row) -> new InventoryResponse(rs.getLong(1), rs.getInt(2), rs.getInt(3)), productId);
        if (rows.isEmpty()) throw new IllegalArgumentException("商品库存不存在");
        return rows.get(0);
    }

    public List<InventoryReplenishmentResponse> history(Long productId) {
        validId(productId);
        return jdbc.query("SELECT * FROM inventory_replenishments WHERE product_id=? ORDER BY id DESC LIMIT 20", ROW, productId);
    }

    @Transactional
    public InventoryReplenishmentResponse replenish(Long productId, InventoryReplenishRequest request, String actor) {
        validId(productId);
        if (request == null || request.requestKey() == null || !request.requestKey().matches("[A-Za-z0-9_-]{8,64}")
                || request.quantity() == null || request.quantity() < 1 || request.quantity() > 1000000
                || request.reason() == null || request.reason().isBlank() || request.reason().length() > 200
                || actor == null || actor.isBlank() || actor.length() > 100) {
            throw new IllegalArgumentException("补货参数不符合要求");
        }
        // 与管理员删除/修改商品串行；同一商品并发补货在此排队。
        var products = jdbc.queryForList("SELECT id FROM products WHERE id=? AND deleted=0 FOR UPDATE", Long.class, productId);
        if (products.isEmpty()) throw new IllegalArgumentException("商品不存在或已删除");
        var existing = jdbc.query("SELECT * FROM inventory_replenishments WHERE actor=? AND request_key=?", ROW, actor, request.requestKey());
        if (!existing.isEmpty()) {
            var saved = existing.get(0);
            if (!saved.productId().equals(productId) || saved.quantity() != request.quantity()
                    || !saved.reason().equals(request.reason().trim())) {
                throw new InventoryConflictException("同一请求编号不能用于不同的补货内容");
            }
            return saved;
        }
        var stock = jdbc.query("SELECT product_id,available_quantity,reserved_quantity FROM product_inventory WHERE product_id=? FOR UPDATE",
                (rs, row) -> new InventoryResponse(rs.getLong(1), rs.getInt(2), rs.getInt(3)), productId);
        if (stock.isEmpty()) throw new InventoryConflictException("商品尚未初始化库存");
        int rows = jdbc.update("""
                UPDATE product_inventory SET available_quantity=available_quantity+?
                WHERE product_id=? AND available_quantity <= 2147483647-?
                """, request.quantity(), productId, request.quantity());
        if (rows != 1) throw new InventoryConflictException("库存数量超过允许上限");
        try {
            jdbc.update("""
                INSERT INTO inventory_replenishments(product_id,actor,request_key,quantity,reason,available_quantity,reserved_quantity)
                VALUES(?,?,?,?,?,?,?)
                """, productId, actor, request.requestKey(), request.quantity(), request.reason().trim(),
                    stock.get(0).availableQuantity() + request.quantity(), stock.get(0).reservedQuantity());
        } catch (DuplicateKeyException conflict) {
            // 不吞掉异常后继续提交；库存更新必须与审计记录一起回滚。
            throw new InventoryConflictException("补货请求编号已用于其他操作，请核对记录");
        }
        return jdbc.query("SELECT * FROM inventory_replenishments WHERE actor=? AND request_key=?", ROW, actor, request.requestKey()).get(0);
    }

    private static void validId(Long id) {
        if (id == null || id <= 0) throw new IllegalArgumentException("商品编号必须为正整数");
    }
}
