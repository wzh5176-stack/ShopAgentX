package com.wwzyhy.shopagent.service;

import com.wwzyhy.shopagent.dto.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderCreateServiceTest {
    private final OrderRequestService requests = mock(OrderRequestService.class);
    private final CurrentUserService users = mock(CurrentUserService.class);
    private final OrderReadService reads = mock(OrderReadService.class);
    private final OrderWriteService writes = mock(OrderWriteService.class);
    private final OrderCreateService service = new OrderCreateService(requests, users, reads, writes);
    private final OrderCreateRequest request = new OrderCreateRequest("key", 1L, List.of());
    private final PreparedOrderRequest prepared = new PreparedOrderRequest("key", 1L, List.of(), "hash");
    private final OrderDetailResponse order = mock(OrderDetailResponse.class);

    private void arrange() {
        when(requests.prepare(request)).thenReturn(prepared);
        when(users.requireUserId(null)).thenReturn(7L);
    }

    @Test void replayReturnsOriginalWithoutReservingAgain() {
        arrange();
        when(reads.findByRequest(7L, prepared)).thenReturn(order);
        assertFalse(service.create(null, request).created());
        verifyNoInteractions(writes);
    }

    @Test void newRequestUsesAuthenticatedUser() {
        arrange();
        when(writes.create(7L, prepared)).thenReturn(order);
        var result = service.create(null, request);
        assertTrue(result.created());
        assertSame(order, result.data());
        verify(writes).create(7L, prepared);
    }

    @Test void concurrentDuplicateReturnsWinningOrder() {
        arrange();
        when(reads.findByRequest(7L, prepared)).thenReturn(null, order);
        when(writes.create(7L, prepared)).thenThrow(new DuplicateKeyException("test duplicate"));
        var result = service.create(null, request);
        assertFalse(result.created());
        assertSame(order, result.data());
        verify(writes, times(1)).create(7L, prepared);
    }

    @Test void duplicateWithoutWinningOrderIsNotHidden() {
        arrange();
        when(writes.create(7L, prepared)).thenThrow(new DuplicateKeyException("test duplicate"));
        assertThrows(DuplicateKeyException.class, () -> service.create(null, request));
    }
}
