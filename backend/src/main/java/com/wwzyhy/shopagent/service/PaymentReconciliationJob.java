package com.wwzyhy.shopagent.service;

import org.springframework.scheduling.annotation.Scheduled;

public class PaymentReconciliationJob {
    private final PaymentReconciliationService service;
    public PaymentReconciliationJob(PaymentReconciliationService service) { this.service = service; }

    @Scheduled(fixedDelayString = "${shopagent.payments.reconcile.delay-ms:30000}",
            initialDelayString = "${shopagent.payments.reconcile.initial-delay-ms:30000}")
    public void run() { service.reconcilePending(); }
}
