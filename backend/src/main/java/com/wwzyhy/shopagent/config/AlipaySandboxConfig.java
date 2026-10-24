package com.wwzyhy.shopagent.config;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.DefaultAlipayClient;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import java.security.Security;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        name = "shopagent.alipay.sandbox.enabled",
        havingValue = "true")
@PropertySource(
        value = "file:${shopagent.alipay.sandbox.config-file:D:/ShopAgent-X/secrets/alipay-sandbox/application.properties}",
        encoding = "UTF-8")
public class AlipaySandboxConfig {

    @Bean("alipaySandboxClient")
    public AlipayClient alipaySandboxClient(
            @Value("${shopagent.alipay.sandbox.app-id}")
            String appId,

            @Value("${shopagent.alipay.sandbox.gateway-url}")
            String gatewayUrl,

            @Value("${shopagent.alipay.sandbox.private-key-file}")
            String privateKeyFile,

            @Value("${shopagent.alipay.sandbox.alipay-public-key-file}")
            String publicKeyFile
    ) throws IOException, AlipayApiException {

        if (!"https://openapi-sandbox.dl.alipaydev.com/gateway.do"
                .equals(gatewayUrl)) {
            throw new IllegalArgumentException(
                    "请使用本组确认的支付宝沙箱网关地址");
        }

        AlipayConfig config = new AlipayConfig();

        config.setServerUrl(gatewayUrl);
        config.setAppId(appId);
        config.setPrivateKey(readKey(privateKeyFile));
        config.setAlipayPublicKey(readKey(publicKeyFile));

        config.setFormat("json");
        config.setCharset("UTF-8");
        config.setSignType("RSA2");

        config.setConnectTimeout(3000);
        config.setReadTimeout(15000);

        String originalPolicy = Security.getProperty(
                "jdk.certpath.disabledAlgorithms");

        try {
            return new DefaultAlipayClient(config);
        } finally {
            if (originalPolicy != null) {
                Security.setProperty(
                        "jdk.certpath.disabledAlgorithms",
                        originalPolicy);
            }
        }
    }

    private String readKey(String file) throws IOException {
        String key = Files.readString(
                        Path.of(file),
                        StandardCharsets.UTF_8)
                .replace("\uFEFF", "")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        if (key.isBlank()) {
            // A missing Alipay public key would leave the SDK without a response verifier.
            throw new IOException("支付密钥材料不能为空");
        }
        return key;
    }
}
