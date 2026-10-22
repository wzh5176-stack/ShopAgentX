package com.wwzyhy.shopagent.controller;

import com.wwzyhy.shopagent.dto.AdminProductDetailResponse;
import com.wwzyhy.shopagent.dto.ProductCreateRequest;
import com.wwzyhy.shopagent.entity.Product;
import com.wwzyhy.shopagent.service.ProductService;
import jakarta.validation.Valid;
import java.math.RoundingMode;
import java.net.URI;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wwzyhy.shopagent.dto.AdminProductPageResponse;
import com.wwzyhy.shopagent.dto.AdminProductSummaryResponse;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.RequestParam;
import com.wwzyhy.shopagent.dto.ProductUpdateRequest;
import org.springframework.web.bind.annotation.PutMapping;
import com.wwzyhy.shopagent.dto.ProductStatusRequest;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;

@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @Valid @RequestBody ProductCreateRequest request) {

        Product product = productService.create(request);

        AdminProductDetailResponse data = new AdminProductDetailResponse(
                product.getId(),
                product.getTitle(),
                product.getBrand(),
                product.getPrice()
                        .setScale(2, RoundingMode.UNNECESSARY)
                        .toPlainString(),
                product.getImageUrl(),
                product.getDescription(),
                product.getStatus()
        );

        URI location = URI.create(
                "/api/admin/products/" + product.getId());

        return ResponseEntity.created(location)
                .body(Map.of("data", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> detail(
            @PathVariable("id") Long id) {

        Product product = productService.findAdminById(id);

        if (product == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", Map.of(
                            "code", "PRODUCT_NOT_FOUND",
                            "message", "商品不存在"
                    )
            ));
        }

        AdminProductDetailResponse data =
                new AdminProductDetailResponse(
                        product.getId(),
                        product.getTitle(),
                        product.getBrand(),
                        product.getPrice()
                                .setScale(2, RoundingMode.UNNECESSARY)
                                .toPlainString(),
                        product.getImageUrl(),
                        product.getDescription(),
                        product.getStatus()
                );

        return ResponseEntity.ok(Map.of("data", data));
    }
    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false) Integer status) {

        Page<Product> result =
                productService.findAdminPage(page, size, keyword, status);

        List<AdminProductSummaryResponse> items = new ArrayList<>();

        for (Product product : result.getRecords()) {
            items.add(new AdminProductSummaryResponse(
                    product.getId(),
                    product.getTitle(),
                    product.getBrand(),
                    product.getPrice()
                            .setScale(2, RoundingMode.UNNECESSARY)
                            .toPlainString(),
                    product.getImageUrl(),
                    product.getStatus()
            ));
        }

        AdminProductPageResponse data = new AdminProductPageResponse(
                items,
                result.getCurrent(),
                result.getSize(),
                result.getTotal(),
                result.getPages()
        );

        return ResponseEntity.ok(Map.of("data", data));
    }


    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody ProductUpdateRequest request) {

        Product product = productService.update(id, request);

        if (product == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", Map.of(
                            "code", "PRODUCT_NOT_FOUND",
                            "message", "商品不存在"
                    )
            ));
        }

        AdminProductDetailResponse data = new AdminProductDetailResponse(
                product.getId(),
                product.getTitle(),
                product.getBrand(),
                product.getPrice()
                        .setScale(2, RoundingMode.UNNECESSARY)
                        .toPlainString(),
                product.getImageUrl(),
                product.getDescription(),
                product.getStatus()
        );

        return ResponseEntity.ok(Map.of("data", data));
    }
    @PatchMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody ProductStatusRequest request) {

        Product product = productService.updateStatus(id, request.status());

        if (product == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", Map.of(
                            "code", "PRODUCT_NOT_FOUND",
                            "message", "商品不存在"
                    )
            ));
        }

        AdminProductDetailResponse data = new AdminProductDetailResponse(
                product.getId(),
                product.getTitle(),
                product.getBrand(),
                product.getPrice()
                        .setScale(2, RoundingMode.UNNECESSARY)
                        .toPlainString(),
                product.getImageUrl(),
                product.getDescription(),
                product.getStatus()
        );

        return ResponseEntity.ok(Map.of("data", data));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(
            @PathVariable("id") Long id) {

        boolean deleted = productService.delete(id);

        if (!deleted) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", Map.of(
                            "code", "PRODUCT_NOT_FOUND",
                            "message", "商品不存在"
                    )
            ));
        }

        return ResponseEntity.noContent().build();
    }
}