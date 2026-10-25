package com.wwzyhy.shopagent.service;

import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.wwzyhy.shopagent.config.AlipaySandboxConfig;
import com.wwzyhy.shopagent.entity.PaymentAttempt;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AlipayNotifyServiceTest {
    @TempDir Path temporary;
    private final AlipayPaymentLookupService lookup = mock(AlipayPaymentLookupService.class);
    private final AlipayPaymentSettlementService settlement = mock(AlipayPaymentSettlementService.class);
    private AlipayNotifyService service;
    private String signingKey;

    @BeforeEach void setup() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var keys = generator.generateKeyPair();
        signingKey = Base64.getEncoder().encodeToString(keys.getPrivate().getEncoded());
        Path privateFile = temporary.resolve("fixture-private.txt");
        Path publicFile = temporary.resolve("fixture-public.txt");
        Files.writeString(privateFile, signingKey);
        Files.writeString(publicFile, Base64.getEncoder().encodeToString(keys.getPublic().getEncoded()));
        String policy = Security.getProperty("jdk.certpath.disabledAlgorithms");
        // 本地生成的测试密钥；只构造 SDK 验签器，不发请求，也不读取用户 secrets。
        AlipayClient client = new AlipaySandboxConfig().alipaySandboxClient("test_app",
                "https://openapi-sandbox.dl.alipaydev.com/gateway.do", privateFile.toString(), publicFile.toString());
        assertEquals(policy, Security.getProperty("jdk.certpath.disabledAlgorithms"));
        Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        service = new AlipayNotifyService(client, lookup, settlement, clock, "test_app", "test_seller");
        PaymentAttempt payment = new PaymentAttempt();
        payment.setId(10L); payment.setOrderId(20L); payment.setAmount(new BigDecimal("19.90"));
        payment.setProviderAppId("test_app"); payment.setProviderSellerId("test_seller");
        payment.setCreatedAt(LocalDateTime.of(2025, 12, 31, 23, 55));
        when(lookup.findByTradeNo("fixture_trade")).thenReturn(payment);
    }

    private Map<String, String> signed(String amount) throws Exception {
        var fields = new HashMap<String, String>();
        fields.put("sign_type", "RSA2"); fields.put("charset", "utf-8"); fields.put("version", "1.0");
        fields.put("notify_type", "trade_status_sync"); fields.put("app_id", "test_app");
        fields.put("seller_id", "test_seller"); fields.put("notify_id", "fixture_notify");
        fields.put("out_trade_no", "fixture_trade"); fields.put("trade_no", "fixture_provider");
        fields.put("total_amount", amount); fields.put("trade_status", "TRADE_SUCCESS");
        fields.put("gmt_payment", "2026-01-01 08:00:00");
        String content = AlipaySignature.getSignCheckContentV1(new HashMap<>(fields));
        fields.put("sign", AlipaySignature.rsaSign(content, signingKey, "UTF-8", "RSA2"));
        return fields;
    }

    @Test void validSignatureAndIdentityReachSettlement() throws Exception {
        service.handle(signed("19.90"));
        verify(settlement).reconcile(eq(10L), argThat(result -> result.tradeStatus().isPaid()
                && new BigDecimal("19.90").compareTo(result.amount()) == 0));
    }
    @Test void tamperedSignatureNeverReachesSettlement() throws Exception {
        var fields = signed("19.90"); fields.put("total_amount", "0.01");
        assertThrows(IllegalArgumentException.class, () -> service.handle(fields));
        verifyNoInteractions(settlement);
    }
    @Test void signedWrongAmountNeverReachesSettlement() throws Exception {
        var fields = signed("0.01");
        assertThrows(IllegalArgumentException.class, () -> service.handle(fields));
        verifyNoInteractions(settlement);
    }
    @Test void missingSignatureNeverReachesLookupOrSettlement() throws Exception {
        var fields = signed("19.90"); fields.remove("sign");
        assertThrows(IllegalArgumentException.class, () -> service.handle(fields));
        verifyNoInteractions(settlement);
        verify(lookup, never()).findByTradeNo(anyString());
    }
}
