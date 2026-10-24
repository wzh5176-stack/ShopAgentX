package com.wwzyhy.shopagent.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "shopagent.orders.expiry.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class OrderExpiryJob {

    private final OrderExpiryService expiryService;

    public OrderExpiryJob(OrderExpiryService expiryService) {
        this.expiryService = expiryService;
    }

    @Scheduled(
            fixedDelayString = "${shopagent.orders.expiry.delay-ms:30000}",
            initialDelayString = "${shopagent.orders.expiry.initial-delay-ms:30000}")
    public void run() {
        expiryService.expireDueOrders();
    }
}
