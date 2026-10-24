# 官方沙箱第二组：本地材料与SDK初始化（手写）

2026-10-24。用户截图确认产品列表有电脑网站支付，应用选择系统默认密钥、公钥模式已启用；用户进一步确认“查看”中有应用私钥和支付宝公钥。现在按普通公钥模式、保留当前系统默认密钥推进，不重新生成覆盖。本组由用户手动保存材料和修改代码；助手未读取密钥、创建私密文件、改正式POM/Java、启动服务或调用官方接口。

## 1. 在本机保存三份文件

用资源管理器在D:\ShopAgent-X\secrets下新建alipay-sandbox文件夹，再保存：

```text
D:\ShopAgent-X\secrets\alipay-sandbox\app-private-key.pem
D:\ShopAgent-X\secrets\alipay-sandbox\alipay-public-key.pem
D:\ShopAgent-X\secrets\alipay-sandbox\application.properties
```

第一份粘贴当前沙箱应用“查看”中的应用私钥，Java/PKCS8格式；第二份粘贴同一沙箱应用提供的支付宝公钥。保存UTF-8文本，确认实际文件名没有多出来.txt后缀。可以保存纯Base64，也可以保留对应的BEGIN PRIVATE KEY/PUBLIC KEY头尾，下面代码会清理头尾和空白。不要把应用公钥误存成支付宝公钥，也不要把材料发到聊天。

项目根.gitignore已有secrets/及*.pem规则，因此此目录被Git忽略；不需要把任何真实密钥写到Java/application.yml/pom.xml或当前教案里。助手后续检查不读取这里的私钥正文。

application.properties完整模板（APPID由用户本机填写）：

```properties
shopagent.alipay.sandbox.app-id=替换成当前沙箱应用的APPID
shopagent.alipay.sandbox.gateway-url=https://openapi-sandbox.dl.alipaydev.com/gateway.do
shopagent.alipay.sandbox.private-key-file=D:/ShopAgent-X/secrets/alipay-sandbox/app-private-key.pem
shopagent.alipay.sandbox.alipay-public-key-file=D:/ShopAgent-X/secrets/alipay-sandbox/alipay-public-key.pem
```

properties用key=value保存配置；路径使用正斜杠，避免反斜杠被当成转义。网关来自用户本轮截图；若控制台以后显示不同网关，先核对并同步代码校验，不套用生产网关或旧教程地址。材料填写好后才启用下文环境变量。

## 2. 给backend/pom.xml增加官方依赖

完整路径D:\ShopAgent-X\backend\pom.xml。在已有dependencies里面追加一次，保留原依赖：

```xml
<dependency>
    <groupId>com.alipay.sdk</groupId>
    <artifactId>alipay-sdk-java</artifactId>
    <version>4.40.1044.ALL</version>
    <exclusions>
        <exclusion>
            <groupId>commons-logging</groupId>
            <artifactId>commons-logging</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

点击IDEA的Maven同步。这里固定官方仓库当前v2 README和POM列出的版本，不声称全平台最新；本轮核Maven Central该版本POM HTTP200。SDK提供AlipayClient/AlipayConfig等类。排除旧commons-logging，继续使用Spring Boot现有的日志桥。

## 3. 新建AlipaySandboxConfig.java

完整路径D:\ShopAgent-X\backend\src\main\java\com\wwzyhy\shopagent\config\AlipaySandboxConfig.java。

```java
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
            @Value("${shopagent.alipay.sandbox.app-id}") String appId,
            @Value("${shopagent.alipay.sandbox.gateway-url}") String gatewayUrl,
            @Value("${shopagent.alipay.sandbox.private-key-file}") String privateKeyFile,
            @Value("${shopagent.alipay.sandbox.alipay-public-key-file}") String publicKeyFile
    ) throws IOException, AlipayApiException {
        if (!"https://openapi-sandbox.dl.alipaydev.com/gateway.do".equals(gatewayUrl)) {
            throw new IllegalArgumentException("请使用本组确认的支付宝沙箱网关地址");
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

        return new DefaultAlipayClient(config);
    }

    private String readKey(String file) throws IOException {
        return Files.readString(Path.of(file), StandardCharsets.UTF_8)
                .replace("\uFEFF", "")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
    }
}
```

### 代码怎么工作

- @Configuration：Spring启动时识别这份配置；proxyBeanMethods=false表示本类不需要Bean方法间代理调用。
- @ConditionalOnProperty：只有shopagent.alipay.sandbox.enabled=true才加载；启用开关放IDEA环境变量，不放在这个类自己还没加载的私密配置文件中。
- @PropertySource：从指定的本地properties加载配置；默认D盘路径，也可用SHOPAGENT_ALIPAY_SANDBOX_CONFIG_FILE覆盖。此文件保存APPID/网关/密钥路径，不把密钥正文放进公开配置。
- @Value：读取配置项，注入Bean方法参数，不需要浏览器传入。Spring配置名与Java变量名各自有作用，例如private-key-file的值传入privateKeyFile。
- readKey：把文件路径变成Path，读UTF-8文本，去BOM、支持的PEM头尾及空白，得到SDK所需密钥字符串；不打印内容。文件实际为PKCS1的RSA PRIVATE KEY则不属于本组格式，需使用页面提供的Java/PKCS8材料。
- AlipayConfig/DefaultAlipayClient属于SDK；AlipaySandboxConfig/readKey是项目自己写的。前者保存网关/身份/签名参数，再交给后者构建SDK客户端。
- RSA2用于签名/验签；json为API报文配置，并不表示电脑网站支付未来不会返回HTML表单。
- timeout单位毫秒，连接3秒/读取15秒，避免后续请求无期限等待。
- return把客户端交给Spring管理，@Bean名为alipaySandboxClient。后续Service用@Qualifier("alipaySandboxClient")注入，不用每次new。创建客户端不是创建支付记录，也不等于已向支付宝发请求。

## 4. IDEA启用沙箱配置

原Java运行配置的环境变量添加，保留已有其他配置：

```text
SHOPAGENT_ALIPAY_SANDBOX_ENABLED=true
```

本组不需要把SHOPAGENT_PAYMENTS_MOCK_ENABLED设true；两个开关职责不同。前者只初始化官方沙箱Client，后者是之前可以人工选成功/失败的MOCK演示入口。现在原POST /api/payments仍只有MOCK创建逻辑，尚未接官方渠道；开启Client不自动改变旧接口路由。

先完成文件/Maven同步即可，几个相关步骤后集中构建检查，不要求这一步就反复启动所有服务。启用后本地配置/密钥路径不存在会导致启动失败，文件齐全再启用。下一组先验证本地材料/签名用法，再接官方渠道、交易标识与电脑网页支付表单；官方真实调用、通知验签/查单、取消关单协调与页面迁移仍待完成。

## 状态与依据

本轮只有只读源码/忽略规则/截图核对、官方文档查阅和Maven Central POM存在性核对，未代写或运行正式候选、未下载SDK完整依赖或验证用户私钥，未做任何支付调用。

官方依据：[v2 README](https://github.com/alipay/alipay-sdk-java-all/blob/master/v2/README.md)、[官方POM](https://github.com/alipay/alipay-sdk-java-all/blob/master/v2/pom.xml)、[DefaultAlipayClient源码](https://github.com/alipay/alipay-sdk-java-all/blob/master/v2/src/main/java/com/alipay/api/DefaultAlipayClient.java)、[AlipayConfig源码](https://github.com/alipay/alipay-sdk-java-all/blob/master/v2/src/main/java/com/alipay/api/AlipayConfig.java)。网关以用户本轮控制台截图为准，官方SDK源码注释里的旧沙箱地址不覆盖控制台新地址。
