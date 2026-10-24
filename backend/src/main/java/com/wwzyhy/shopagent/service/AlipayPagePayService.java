package com.wwzyhy.shopagent.service;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.wwzyhy.shopagent.exception.PaymentConflictException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

@Service
@ConditionalOnProperty(
        name = "shopagent.alipay.sandbox.enabled",
        havingValue = "true")
public class AlipayPagePayService {

    private static final DateTimeFormatter ALIPAY_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AlipayClient client;
    private final Clock clock;
    private String notifyUrl = "";
    private String returnBaseUrl = "";

    @Autowired
    public void configureCallbacks(
            @Value("${shopagent.alipay.sandbox.notify-url:}") String notifyUrl,
            @Value("${shopagent.alipay.sandbox.return-base-url:}") String returnBaseUrl) {
        if (!notifyUrl.isBlank()) {
            URI u = URI.create(notifyUrl);
            if (!"https".equals(u.getScheme()) || u.getHost() == null || u.getUserInfo() != null
                    || u.getFragment() != null || u.getQuery() != null
                    || !"/api/payments/alipay/notify".equals(u.getPath())) {
                throw new IllegalArgumentException("通知地址应为HTTPS地址并指向支付通知接口");
            }
        }
        if (!returnBaseUrl.isBlank()) {
            URI u = URI.create(returnBaseUrl);
            if (!("https".equals(u.getScheme()) || "http".equals(u.getScheme())) || u.getHost() == null
                    || u.getUserInfo() != null || u.getFragment() != null || u.getQuery() != null
                    || !(u.getPath().isEmpty() || "/".equals(u.getPath()))) {
                throw new IllegalArgumentException("返回地址应为本站的完整来源地址");
            }
        }
        this.notifyUrl = notifyUrl;
        this.returnBaseUrl = returnBaseUrl.replaceAll("/+$", "");
    }

    public AlipayPagePayService(
            @Qualifier("alipaySandboxClient") AlipayClient client,
            Clock clock) {
        this.client = client;
        this.clock = clock;
    }

    public String createUrl(
            String outTradeNo,
            Long orderId,
            BigDecimal amount,
            LocalDateTime expiresAt) {

        if (outTradeNo == null
                || !outTradeNo.matches("[A-Za-z0-9_]{1,64}")
                || orderId == null
                || orderId <= 0
                || amount == null
                || amount.compareTo(new BigDecimal("0.01")) < 0
                || amount.compareTo(new BigDecimal("100000000")) > 0
                || expiresAt == null) {
            throw new IllegalArgumentException(
                    "沙箱支付参数不符合要求");
        }

        LocalDateTime now = LocalDateTime.now(clock);

        if (!expiresAt.isAfter(now.plusSeconds(61))
                || expiresAt.isAfter(now.plusDays(15))) {
            throw new PaymentConflictException(
                    "订单剩余支付时间不符合沙箱要求");
        }

        AlipayTradePagePayModel model =
                new AlipayTradePagePayModel();

        model.setOutTradeNo(outTradeNo);
        model.setSubject("ShopAgent订单" + orderId);
        model.setProductCode("FAST_INSTANT_TRADE_PAY");

        model.setTotalAmount(
                amount.setScale(2, RoundingMode.UNNECESSARY)
                        .toPlainString());

        model.setTimeExpire(
                expiresAt.atZone(clock.getZone())
                        .withZoneSameInstant(
                                ZoneId.of("Asia/Shanghai"))
                        .format(ALIPAY_TIME));

        AlipayTradePagePayRequest request =
                new AlipayTradePagePayRequest();

        request.setBizModel(model);
        if (!notifyUrl.isBlank()) { request.setNotifyUrl(notifyUrl); }
        if (!returnBaseUrl.isBlank()) {
            request.setReturnUrl(returnBaseUrl + "/shopagent/order.html?id=" + orderId);
        }

        String url;

        try {
            url = client.pageExecute(request, "GET")
                    .getBody();
        } catch (AlipayApiException exception) {
            throw new IllegalStateException(
                    "生成支付宝沙箱付款链接失败");
        }

        if (url == null || url.isBlank()) {
            throw new IllegalStateException(
                    "SDK未返回沙箱付款链接");
        }

        URI target = URI.create(url);

        if (!"https".equals(target.getScheme())
                || !"openapi-sandbox.dl.alipaydev.com"
                .equals(target.getHost())) {
            throw new IllegalStateException(
                    "付款链接不是本组确认的沙箱地址");
        }

        return url;
    }
}
