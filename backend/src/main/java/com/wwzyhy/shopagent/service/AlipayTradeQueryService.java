package com.wwzyhy.shopagent.service;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.internal.util.AlipayUtils;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.wwzyhy.shopagent.dto.AlipayQueryResult;
import com.wwzyhy.shopagent.dto.AlipayQueryResult.TradeStatus;
import com.wwzyhy.shopagent.entity.PaymentAttempt;
import com.wwzyhy.shopagent.exception.AlipayQueryUnavailableException;
import com.wwzyhy.shopagent.exception.PaymentConflictException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
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
public class AlipayTradeQueryService {
    private static final ZoneId ALIPAY_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter PAYMENT_TIME = DateTimeFormatter
            .ofPattern("uuuu-MM-dd HH:mm:ss", Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT);

    private final AlipayClient client;
    private final Clock clock;
    private final String appId;
    private final String sellerId;

    public AlipayTradeQueryService(
            @Qualifier("alipaySandboxClient") AlipayClient client,
            Clock clock,
            @Value("${shopagent.alipay.sandbox.app-id}") String appId,
            @Value("${shopagent.alipay.sandbox.seller-id}") String sellerId) {
        this.client = client;
        this.clock = clock;
        this.appId = appId;
        this.sellerId = sellerId;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AlipayQueryResult query(PaymentAttempt payment) {
        if (payment == null || !"ALIPAY_SANDBOX".equals(payment.getChannel())
                || !appId.equals(payment.getProviderAppId())
                || !sellerId.equals(payment.getProviderSellerId())
                || payment.getOutTradeNo() == null
                || !payment.getOutTradeNo().matches("[A-Za-z0-9_]{1,64}")) {
            throw new PaymentConflictException("支付单渠道或应用商家身份与当前配置不一致");
        }

        AlipayTradeQueryModel model = new AlipayTradeQueryModel();
        model.setOutTradeNo(payment.getOutTradeNo());
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        request.setBizModel(model);

        try {
            // The configured SDK verifies successful responses with the Alipay public key.
            AlipayTradeQueryResponse response = client.execute(request);
            if (response == null) {
                throw new AlipayQueryUnavailableException();
            }
            if (!response.isSuccess()) {
                if ("40004".equals(response.getCode())
                        && "ACQ.TRADE_NOT_EXIST".equals(response.getSubCode())) {
                    // A cashier link may not yet have been opened. This is not proof of closure.
                    return result(payment, TradeStatus.NOT_FOUND, null, null, null);
                }
                throw new AlipayQueryUnavailableException();
            }

            if (response.getTradeStatus() == null) {
                throw new AlipayQueryUnavailableException();
            }
            TradeStatus status = TradeStatus.valueOf(response.getTradeStatus());
            if (status == TradeStatus.NOT_FOUND
                    || !payment.getOutTradeNo().equals(response.getOutTradeNo())
                    || response.getTradeNo() == null
                    || !response.getTradeNo().matches("[A-Za-z0-9_]{1,64}")
                    || response.getTotalAmount() == null
                    || !response.getTotalAmount().matches("[0-9]{1,9}(\\.[0-9]{1,2})?")) {
                throw new AlipayQueryUnavailableException();
            }
            BigDecimal amount = new BigDecimal(response.getTotalAmount());
            if (payment.getAmount().compareTo(amount) != 0) {
                throw new AlipayQueryUnavailableException();
            }
            LocalDateTime paidAt = status.isPaid() ? readPaidAt(response) : null;
            if (paidAt != null && (paidAt.isAfter(LocalDateTime.now(clock).plusMinutes(5))
                    || paidAt.isBefore(payment.getCreatedAt().minusMinutes(5)))) {
                throw new AlipayQueryUnavailableException();
            }
            return result(payment, status, amount, response.getTradeNo(), paidAt);
        } catch (AlipayApiException | RuntimeException exception) {
            // Some malformed/unsigned SDK responses throw RuntimeException rather than AlipayApiException.
            throw new AlipayQueryUnavailableException();
        }
    }

    private LocalDateTime readPaidAt(AlipayTradeQueryResponse response) {
        // Read the signed raw string: SDK Date parsing can otherwise depend on the JVM timezone.
        Map<?, ?> envelope = AlipayUtils.parseJson(response.getBody());
        if (envelope == null
                || !(envelope.get("alipay_trade_query_response") instanceof Map<?, ?> body)
                || !(body.get("send_pay_date") instanceof String value)) {
            throw new AlipayQueryUnavailableException();
        }
        return LocalDateTime.parse(value, PAYMENT_TIME).atZone(ALIPAY_ZONE)
                .withZoneSameInstant(clock.getZone()).toLocalDateTime();
    }

    private AlipayQueryResult result(PaymentAttempt payment, TradeStatus status,
            BigDecimal amount, String tradeNo, LocalDateTime paidAt) {
        // Query responses do not echo app_id/seller_id: these identify our configured direct client.
        return new AlipayQueryResult(payment.getId(), payment.getOrderId(),
                payment.getOutTradeNo(), appId, sellerId, status, amount, tradeNo, paidAt);
    }
}
