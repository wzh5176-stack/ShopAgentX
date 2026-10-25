package com.wwzyhy.shopagent.service;

import com.wwzyhy.shopagent.dto.*;
import com.wwzyhy.shopagent.entity.Product;
import com.wwzyhy.shopagent.entity.ProductInventory;
import com.wwzyhy.shopagent.exception.CheckoutUnavailableException;
import com.wwzyhy.shopagent.mapper.ProductInventoryMapper;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CheckoutServiceTest {
    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private final AddressService addresses = mock(AddressService.class);
    private final ProductService products = mock(ProductService.class);
    private final ProductInventoryMapper inventory = mock(ProductInventoryMapper.class);
    private final CheckoutService service = new CheckoutService(addresses, products, inventory, FACTORY.getValidator());
    @AfterAll static void closeValidator() { FACTORY.close(); }

    private CheckoutPreviewRequest request() {
        return new CheckoutPreviewRequest(1L, List.of(new CheckoutItemRequest(9L, 3)));
    }
    private void arrangeProduct(int available) {
        Product product = new Product();
        product.setId(9L); product.setTitle("测试耳机"); product.setBrand("Demo");
        product.setPrice(new BigDecimal("19.90"));
        ProductInventory stock = new ProductInventory();
        stock.setProductId(9L); stock.setAvailableQuantity(available);
        when(products.findOnSaleById(9L)).thenReturn(product);
        when(inventory.selectById(9L)).thenReturn(stock);
    }

    @Test void priceAndTotalComeFromServerAndUseDecimalArithmetic() {
        arrangeProduct(3);
        var result = service.preview(null, request());
        assertEquals("59.70", result.totalAmount());
        assertEquals("19.90", result.items().get(0).unitPrice());
        verify(addresses).get(null, 1L);
    }

    @Test void insufficientStockCannotBeQuotedAsAvailable() {
        arrangeProduct(2);
        assertThrows(CheckoutUnavailableException.class, () -> service.preview(null, request()));
    }

    @Test void offSaleProductIsRejected() {
        assertThrows(CheckoutUnavailableException.class, () -> service.preview(null, request()));
        verifyNoInteractions(inventory);
    }

    @Test void duplicateProductIsRejectedBeforeDataAccess() {
        var duplicate = new CheckoutPreviewRequest(1L, List.of(new CheckoutItemRequest(9L, 1), new CheckoutItemRequest(9L, 2)));
        assertThrows(IllegalArgumentException.class, () -> service.preview(null, duplicate));
        verifyNoInteractions(addresses, products, inventory);
    }
}
