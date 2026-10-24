package com.wwzyhy.shopagent.config;

import com.wwzyhy.shopagent.service.AlipayPaymentReconciliationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(name = "shopagent.alipay.sandbox.enabled", havingValue = "true")
public class AlipayReconciliationConfig {
    @Bean
    @ConditionalOnProperty(name = "shopagent.alipay.sandbox.reconcile.enabled", havingValue = "true", matchIfMissing = true)
    public ReconciliationJob alipayReconciliationJob(AlipayPaymentReconciliationService service) {
        return new ReconciliationJob(service);
    }

    public static class ReconciliationJob {
        private final AlipayPaymentReconciliationService service;
        public ReconciliationJob(AlipayPaymentReconciliationService service) { this.service = service; }
        @Scheduled(fixedDelayString = "${shopagent.alipay.sandbox.reconcile.delay-ms:30000}",
                initialDelayString = "${shopagent.alipay.sandbox.reconcile.initial-delay-ms:30000}")
        public void run() { service.reconcilePending(); }
    }
}
