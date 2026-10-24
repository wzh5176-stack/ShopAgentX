package com.wwzyhy.shopagent.service;

import com.wwzyhy.shopagent.dto.OrderDetailResponse;
import com.wwzyhy.shopagent.exception.AlipayCloseRequiredException;
import com.wwzyhy.shopagent.exception.AlipayCloseUnavailableException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderCloseCoordinator {
    private final OrderCloseService localClose;
    private final ObjectProvider<AlipayOrderCloseService> alipayClose;

    public OrderCloseCoordinator(OrderCloseService localClose,
            ObjectProvider<AlipayOrderCloseService> alipayClose) {
        this.localClose = localClose;
        this.alipayClose = alipayClose;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public OrderDetailResponse cancel(Authentication authentication, Long id) {
        try {
            return localClose.cancel(authentication, id);
        } catch (AlipayCloseRequiredException exception) {
            requireAlipay().resolveOwned(authentication, id);
            return localClose.cancel(authentication, id);
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public boolean expire(Long id) {
        try {
            return localClose.expire(id);
        } catch (AlipayCloseRequiredException exception) {
            requireAlipay().resolveForExpiry(id);
            return localClose.expire(id);
        }
    }

    private AlipayOrderCloseService requireAlipay() {
        AlipayOrderCloseService service = alipayClose.getIfAvailable();
        if (service == null) { throw new AlipayCloseUnavailableException(); }
        return service;
    }
}
