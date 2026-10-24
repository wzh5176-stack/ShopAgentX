package com.wwzyhy.shopagent.controller;

import com.wwzyhy.shopagent.dto.PaymentCreateRequest;
import com.wwzyhy.shopagent.exception.PaymentConflictException;
import com.wwzyhy.shopagent.service.AlipayCheckoutService;
import com.wwzyhy.shopagent.service.PaymentReadService;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(name = "shopagent.alipay.sandbox.enabled", havingValue = "true")
public class AlipayResumeController {
    private final PaymentReadService read;
    private final AlipayCheckoutService checkout;
    public AlipayResumeController(PaymentReadService read, AlipayCheckoutService checkout) {
        this.read = read; this.checkout = checkout;
    }
    @PostMapping("/api/payments/alipay/{id}/page")
    public ResponseEntity<Map<String, Object>> resume(@PathVariable("id") Long id, Authentication auth) {
        var payment = read.requireOwned(auth, id);
        if (!"ALIPAY_SANDBOX".equals(payment.getChannel())) {
            throw new PaymentConflictException("不是支付宝沙箱支付单");
        }
        var data = checkout.checkout(auth, new PaymentCreateRequest(payment.getOrderId(), payment.getRequestKey()));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("data", data));
    }
}
