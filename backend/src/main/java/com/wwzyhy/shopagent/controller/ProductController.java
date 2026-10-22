package com.wwzyhy.shopagent.controller;

import com.wwzyhy.shopagent.dto.ProductDetailResponse;
import com.wwzyhy.shopagent.entity.Product;
import com.wwzyhy.shopagent.service.ProductService;
import java.math.RoundingMode;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wwzyhy.shopagent.dto.ProductPageResponse;
import com.wwzyhy.shopagent.dto.ProductSummaryResponse;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> detail(
            @PathVariable("id") Long id) {

        if (id <= 0) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", Map.of(
                            "code", "INVALID_ARGUMENT",
                            "message", "商品编号必须为正整数"
                    )
            ));
        }

        Product product = productService.findOnSaleById(id);

        if (product == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", Map.of(
                            "code", "PRODUCT_NOT_FOUND",
                            "message", "商品不存在或已下架"
                    )
            ));
        }

        ProductDetailResponse data = new ProductDetailResponse(
                product.getId(),
                product.getTitle(),
                product.getBrand(),
                product.getPrice()
                        .setScale(2, RoundingMode.UNNECESSARY)
                        .toPlainString(),
                product.getImageUrl(),
                product.getDescription()
        );

        return ResponseEntity.ok(Map.of("data", data));
    }


    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "keyword", required = false) String keyword) {

        Page<Product> result =
                productService.findOnSalePage(page, size, keyword);

        List<ProductSummaryResponse> items = new ArrayList<>();

        for (Product product : result.getRecords()) {
            items.add(new ProductSummaryResponse(
                    product.getId(),
                    product.getTitle(),
                    product.getBrand(),
                    product.getPrice()
                            .setScale(2, RoundingMode.UNNECESSARY)
                            .toPlainString(),
                    product.getImageUrl()
            ));
        }

        ProductPageResponse data = new ProductPageResponse(
                items,
                result.getCurrent(),
                result.getSize(),
                result.getTotal(),
                result.getPages()
        );

        return ResponseEntity.ok(Map.of("data", data));
    }
}