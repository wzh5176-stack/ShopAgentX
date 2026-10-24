package com.wwzyhy.shopagent.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.Bean;
import com.wwzyhy.shopagent.service.PaymentReconciliationJob;
import com.wwzyhy.shopagent.service.PaymentReconciliationService;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(name = "shopagent.payments.mock.enabled", havingValue = "true")
public class PaymentSchedulingConfig {
    @Bean
    @ConditionalOnProperty(name = "shopagent.payments.reconcile.enabled", havingValue = "true", matchIfMissing = true)
    public PaymentReconciliationJob paymentReconciliationJob(PaymentReconciliationService service) {
        return new PaymentReconciliationJob(service);
    }
}
