package com.wwzyhy.shopagent.service;

import com.alipay.api.AbstractAlipayClient;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.wwzyhy.shopagent.dto.AlipayQueryResult;
import com.wwzyhy.shopagent.dto.AlipayQueryResult.TradeStatus;
import com.wwzyhy.shopagent.entity.PaymentAttempt;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "shopagent.alipay.sandbox.enabled", havingValue = "true")
public class AlipayNotifyService {
    private final AlipayClient client;
    private final AlipayPaymentLookupService lookup;
    private final AlipayPaymentSettlementService settlement;
    private final Clock clock;
    private final String appId;
    private final String sellerId;

    public AlipayNotifyService(@Qualifier("alipaySandboxClient") AlipayClient client,
            AlipayPaymentLookupService lookup, AlipayPaymentSettlementService settlement,
            Clock clock, @Value("${shopagent.alipay.sandbox.app-id}") String appId,
            @Value("${shopagent.alipay.sandbox.seller-id}") String sellerId) {
        this.client = client;
        this.lookup = lookup;
        this.settlement = settlement;
        this.clock = clock;
        this.appId = appId;
        this.sellerId = sellerId;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void handle(Map<String, String> input) {
        Map<String, String> p = Map.copyOf(input);
        if (!"RSA2".equals(p.get("sign_type")) || !"utf-8".equalsIgnoreCase(p.get("charset"))
                || !"1.0".equals(p.get("version")) || !"trade_status_sync".equals(p.get("notify_type"))
                || !appId.equals(p.get("app_id")) || !sellerId.equals(p.get("seller_id"))
                || (p.containsKey("auth_app_id") && !appId.equals(p.get("auth_app_id")))
                || p.getOrDefault("notify_id", "").isBlank() || p.get("notify_id").length() > 128
                || !p.getOrDefault("out_trade_no", "").matches("[A-Za-z0-9_]{1,64}")
                || !p.getOrDefault("trade_no", "").matches("[A-Za-z0-9_]{1,64}")
                || !p.getOrDefault("total_amount", "").matches("[0-9]{1,9}(\\.[0-9]{1,2})?")
                || p.getOrDefault("sign", "").isBlank()) {
            throw rejected();
        }
        try {
            if (!(client instanceof AbstractAlipayClient actual) || actual.getSignChecker() == null
                    || !actual.getSignChecker().check(
                    AlipaySignature.getSignCheckContentV1(new HashMap<>(p)),
                    p.get("sign"), "RSA2", "UTF-8")) {
                throw rejected();
            }
        } catch (RuntimeException exception) {
            throw rejected();
        }
        PaymentAttempt payment = lookup.findByTradeNo(p.get("out_trade_no"));
        BigDecimal amount = new BigDecimal(p.get("total_amount"));
        if (payment == null || !appId.equals(payment.getProviderAppId())
                || !sellerId.equals(payment.getProviderSellerId())
                || payment.getAmount().compareTo(amount) != 0) {
            throw rejected();
        }
        TradeStatus status;
        LocalDateTime paidAt = null;
        try {
            status = TradeStatus.valueOf(p.getOrDefault("trade_status", ""));
            if (status == TradeStatus.NOT_FOUND) { throw rejected(); }
            if (status.isPaid()) {
                var format = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss", Locale.ROOT)
                        .withResolverStyle(ResolverStyle.STRICT);
                paidAt = LocalDateTime.parse(p.getOrDefault("gmt_payment", ""), format)
                        .atZone(ZoneId.of("Asia/Shanghai"))
                        .withZoneSameInstant(clock.getZone()).toLocalDateTime();
                if (paidAt.isBefore(payment.getCreatedAt().minusMinutes(5))
                        || paidAt.isAfter(LocalDateTime.now(clock).plusMinutes(5))) { throw rejected(); }
            }
        } catch (RuntimeException exception) { throw rejected(); }
        // The shared locked transaction implements idempotency, not a browser/session or notify-id cache.
        settlement.reconcile(payment.getId(), new AlipayQueryResult(payment.getId(), payment.getOrderId(),
                payment.getOutTradeNo(), appId, sellerId, status, amount, p.get("trade_no"), paidAt));
    }

    private IllegalArgumentException rejected() { return new IllegalArgumentException("支付通知校验未通过"); }
}
