package com.wwzyhy.shopagent.service;

import com.wwzyhy.shopagent.dto.OrderCreateRequest;
import com.wwzyhy.shopagent.dto.OrderCreateResult;
import com.wwzyhy.shopagent.dto.OrderDetailResponse;
import com.wwzyhy.shopagent.dto.PreparedOrderRequest;
import com.wwzyhy.shopagent.exception.AddressNotFoundException;
import com.wwzyhy.shopagent.exception.CheckoutUnavailableException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class OrderCreateService {

    private final OrderRequestService requestService;
    private final CurrentUserService currentUserService;
    private final OrderReadService readService;
    private final OrderWriteService writeService;

    public OrderCreateService(
            OrderRequestService requestService,
            CurrentUserService currentUserService,
            OrderReadService readService,
            OrderWriteService writeService) {
        this.requestService = requestService;
        this.currentUserService = currentUserService;
        this.readService = readService;
        this.writeService = writeService;
    }

    public OrderCreateResult create(
            Authentication authentication,
            OrderCreateRequest request) {
        PreparedOrderRequest prepared = requestService.prepare(request);
        Long userId = currentUserService.requireUserId(authentication);

        OrderDetailResponse existing =
                readService.findByRequest(userId, prepared);

        if (existing != null) {
            return new OrderCreateResult(false, existing);
        }

        try {
            OrderDetailResponse data = writeService.create(userId, prepared);
            return new OrderCreateResult(true, data);
        } catch (DuplicateKeyException
                 | PessimisticLockingFailureException
                 | CheckoutUnavailableException
                 | AddressNotFoundException exception) {
            existing = readService.findByRequest(userId, prepared);

            if (existing != null) {
                return new OrderCreateResult(false, existing);
            }

            throw exception;
        }
    }
}