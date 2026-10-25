package com.wwzyhy.shopagent.service;

import com.wwzyhy.shopagent.dto.CheckoutItemRequest;
import com.wwzyhy.shopagent.dto.OrderCreateRequest;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class OrderRequestServiceTest {
    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private final OrderRequestService service = new OrderRequestService(FACTORY.getValidator());
    private static final String KEY = "00000000-0000-0000-0000-000000000001";

    @AfterAll static void closeValidator() { FACTORY.close(); }

    @Test void equivalentItemOrderHasSameFingerprint() {
        var first = service.prepare(request(1L, List.of(item(2L, 1), item(1L, 2))));
        var second = service.prepare(request(1L, List.of(item(1L, 2), item(2L, 1))));
        assertEquals(first.requestHash(), second.requestHash());
        assertEquals(1L, first.items().get(0).productId());
    }

    @Test void changingAddressOrQuantityChangesFingerprint() {
        var original = service.prepare(request(1L, List.of(item(1L, 1))));
        assertNotEquals(original.requestHash(), service.prepare(request(2L, List.of(item(1L, 1)))).requestHash());
        assertNotEquals(original.requestHash(), service.prepare(request(1L, List.of(item(1L, 2)))).requestHash());
    }

    @Test void duplicateProductIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.prepare(request(1L, List.of(item(1L, 1), item(1L, 2)))));
    }

    @ParameterizedTest @ValueSource(ints = {-1, 0, 100})
    void invalidQuantityIsRejected(int quantity) {
        assertThrows(IllegalArgumentException.class,
                () -> service.prepare(request(1L, List.of(item(1L, quantity)))));
    }

    @Test void emptyOrMalformedRequestIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.prepare(null));
        assertThrows(IllegalArgumentException.class, () -> service.prepare(request(1L, List.of())));
        assertThrows(IllegalArgumentException.class, () -> service.prepare(
                new OrderCreateRequest("invalid", 1L, List.of(item(1L, 1)))));
    }

    private OrderCreateRequest request(Long address, List<CheckoutItemRequest> items) {
        return new OrderCreateRequest(KEY, address, items);
    }
    private CheckoutItemRequest item(Long id, int quantity) { return new CheckoutItemRequest(id, quantity); }
}
