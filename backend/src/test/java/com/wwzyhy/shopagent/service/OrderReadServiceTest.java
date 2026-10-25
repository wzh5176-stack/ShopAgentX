package com.wwzyhy.shopagent.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.wwzyhy.shopagent.exception.OrderNotFoundException;
import com.wwzyhy.shopagent.mapper.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderReadServiceTest {
    @Test void lookupIncludesBothOrderAndAuthenticatedUser() {
        var users = mock(CurrentUserService.class);
        var orders = mock(ShopOrderMapper.class);
        var items = mock(OrderItemMapper.class);
        when(users.requireUserId(null)).thenReturn(7L);
        var service = new OrderReadService(users, orders, items);
        assertThrows(OrderNotFoundException.class, () -> service.get(null, 99L));
        var query = ArgumentCaptor.forClass(Wrapper.class);
        verify(orders).selectOne(query.capture());
        var wrapper = (com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<?>) query.getValue();
        String sql = wrapper.getSqlSegment();
        assertTrue(sql.contains("user_id"));
        assertTrue(sql.contains("id"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(7L));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(99L));
        verifyNoInteractions(items);
    }
}
