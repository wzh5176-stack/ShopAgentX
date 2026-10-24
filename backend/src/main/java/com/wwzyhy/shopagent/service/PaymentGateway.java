package com.wwzyhy.shopagent.service;

import com.wwzyhy.shopagent.dto.PaymentGatewayResult;
import com.wwzyhy.shopagent.entity.PaymentAttempt;

public interface PaymentGateway {
    String channel();
    // Null means the adapter has no final result yet. Call outside merchant transactions.
    PaymentGatewayResult query(PaymentAttempt payment);
}
