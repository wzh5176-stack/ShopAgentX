package com.wwzyhy.shopagent.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(
        name = "shopagent.orders.expiry.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class OrderSchedulingConfig {
}
