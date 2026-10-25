package com.wwzyhy.shopagent.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wwzyhy.shopagent.entity.Product;
import com.wwzyhy.shopagent.mapper.ProductMapper;
import com.wwzyhy.shopagent.entity.ProductInventory;
import com.wwzyhy.shopagent.mapper.ProductInventoryMapper;
import org.springframework.stereotype.Service;
import com.wwzyhy.shopagent.dto.ProductCreateRequest;
import java.net.URI;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.wwzyhy.shopagent.dto.ProductUpdateRequest;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;


@Service
public class ProductService {

    private final ProductMapper productMapper;
    private final ProductInventoryMapper inventoryMapper;

    public ProductService(
            ProductMapper productMapper,
            ProductInventoryMapper inventoryMapper) {
        this.productMapper = productMapper;
        this.inventoryMapper = inventoryMapper;
    }

    public Product findOnSaleById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("商品编号必须为正整数");
        }

        QueryWrapper<Product> query = new QueryWrapper<>();
        query.eq("id", id);
        query.eq("status", 1);

        return productMapper.selectOne(query);
    }

    public Page<Product> findOnSalePage(
            int page, int size, String keyword) {

        if (page < 1) {
            throw new IllegalArgumentException("页码必须大于等于1");
        }

        if (size < 1 || size > 50) {
            throw new IllegalArgumentException("每页数量必须在1到50之间");
        }

        String searchKeyword = keyword == null ? "" : keyword.trim();

        if (searchKeyword.length() > 50) {
            throw new IllegalArgumentException("关键词不能超过50个字符");
        }

        QueryWrapper<Product> query = new QueryWrapper<>();
        query.eq("status", 1);

        if (!searchKeyword.isEmpty()) {
            query.and(group -> group
                    .like("title", searchKeyword)
                    .or()
                    .like("brand", searchKeyword));
        }

        query.orderByAsc("id");

        Page<Product> pageRequest = new Page<>(page, size);
        return productMapper.selectPage(pageRequest, query);
    }

    @Transactional
    public Product create(ProductCreateRequest request) {
        Product product = new Product();

        product.setTitle(request.title().trim());
        product.setBrand(request.brand().trim());
        product.setPrice(request.price());
        product.setImageUrl(normalizeImageUrl(request.imageUrl()));
        product.setDescription(request.description());
        product.setStatus(request.status());
        product.setDeleted(0);

        int rows = productMapper.insert(product);
        if (rows != 1 || product.getId() == null) {
            throw new IllegalStateException("新增商品失败");
        }

        ProductInventory inventory = new ProductInventory();
        inventory.setProductId(product.getId());
        inventory.setAvailableQuantity(0);
        inventory.setReservedQuantity(0);
        if (inventoryMapper.insert(inventory) != 1) {
            throw new IllegalStateException("初始化商品库存失败");
        }

        return product;
    }

    private String normalizeImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return null;
        }

        String normalized = imageUrl.trim();

        if (normalized.matches("/shopagent/catalog/[a-z][0-9]{2}\\.svg")) {
            return normalized;
        }

        URI uri;
        try {
            uri = URI.create(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "图片地址格式不正确", exception);
        }

        if (!("http".equalsIgnoreCase(uri.getScheme())
                || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null) {
            throw new IllegalArgumentException(
                    "图片地址必须是HTTP或HTTPS地址并包含主机名");
        }

        return normalized;
    }

    public Product findAdminById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("商品编号必须为正整数");
        }

        return productMapper.selectById(id);
    }

    public Page<Product> findAdminPage(
            int page, int size, String keyword, Integer status) {

        if (page < 1 || size < 1 || size > 50) {
            throw new IllegalArgumentException("分页参数不符合要求");
        }

        if (status != null && status != 0 && status != 1) {
            throw new IllegalArgumentException("商品状态只能是0或1");
        }

        String searchKeyword = keyword == null ? "" : keyword.trim();

        if (searchKeyword.length() > 50) {
            throw new IllegalArgumentException("关键词不能超过50个字符");
        }

        QueryWrapper<Product> query = new QueryWrapper<>();

        if (status != null) {
            query.eq("status", status);
        }

        if (!searchKeyword.isEmpty()) {
            query.and(group -> group
                    .like("title", searchKeyword)
                    .or()
                    .like("brand", searchKeyword));
        }

        query.orderByAsc("id");

        Page<Product> pageRequest = new Page<>(page, size);
        return productMapper.selectPage(pageRequest, query);
    }

    @Transactional
    public Product update(Long id, ProductUpdateRequest request) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("商品编号必须为正整数");
        }

        UpdateWrapper<Product> update = new UpdateWrapper<>();

        update.eq("id", id);

        update.set("title", request.title().trim());
        update.set("brand", request.brand().trim());
        update.set("price", request.price());
        update.set("image_url", normalizeImageUrl(request.imageUrl()));
        update.set("description", request.description());
        update.set("status", request.status());

        int rows = productMapper.update(null, update);

        if (rows == 0) {
            return null;
        }

        return productMapper.selectById(id);
    }


    @Transactional
    public Product updateStatus(Long id, Integer status) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("商品编号必须为正整数");
        }

        if (status == null || (status != 0 && status != 1)) {
            throw new IllegalArgumentException("商品状态只能是0或1");
        }

        UpdateWrapper<Product> update = new UpdateWrapper<>();

        update.eq("id", id);
        update.set("status", status);

        int rows = productMapper.update(null, update);

        if (rows == 0) {
            return null;
        }

        return productMapper.selectById(id);
    }
    public boolean delete(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("商品编号必须为正整数");
        }

        return productMapper.deleteById(id) == 1;
    }

    public List<Product> findCandidates(String keyword, BigDecimal maxPrice) {
        String searchKeyword = keyword == null ? "" : keyword.trim();
        if (searchKeyword.isEmpty()) {
            return List.of();
        }
        if (searchKeyword.length() > 50) {
            throw new IllegalArgumentException("关键词不能超过50个字符");
        }
        if (maxPrice != null && (maxPrice.signum() < 0
                || maxPrice.compareTo(new BigDecimal("99999999.99")) > 0)) {
            throw new IllegalArgumentException("最高预算超出有效范围");
        }

        QueryWrapper<Product> query = new QueryWrapper<>();
        query.eq("status", 1);
        query.and(group -> group
                .like("title", searchKeyword)
                .or()
                .like("brand", searchKeyword));

        if (maxPrice != null) {
            query.le("price", maxPrice);
        }
        query.inSql("id", "SELECT product_id FROM product_inventory WHERE available_quantity > 0");
        query.orderByAsc("id");

        Page<Product> pageRequest = new Page<>(1, 20, false);
        return productMapper.selectPage(pageRequest, query).getRecords();
    }
}

