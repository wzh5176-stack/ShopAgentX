# 支付宝查单与成功结算：本机验收

2026-10-24，用户明确要求“你直接帮我搞了这一步”，并在确认均为 IDEA 中 Java 开发后继续。本组已直接实现和集中验收。下一阶段是官方关单/取消过期协调，随后通知验签、交易页面和真实沙箱验收。

## 当前能力与调用链

沿用 `POST /api/payments/{id}/refresh`，USER + CSRF + 本人归属不变。

```text
PaymentController.refresh
  → PaymentService.refresh：读取本人支付单，按渠道分流
    → MOCK：原查询与结算服务
    → ALIPAY_SANDBOX：AlipayPaymentRefreshService
      → PaymentReadService.requireOwned：独立新事务读取归属
      → AlipayTradeQueryService.query：事务外 SDK 查询与验证
      → AlipayPaymentSettlementService.apply：独立新事务锁订单、锁支付
        → 核对交易号、金额与配置身份绑定
        → 按商品 ID 顺序消费预占库存
        → 订单 PAID，支付 SUCCEEDED，保存官方交易号与付款时间
        → 一起提交；任一步失败一起回滚
```

新增 5 个文件：

- `backend/src/main/java/com/wwzyhy/shopagent/dto/AlipayQueryResult.java`：内部查询结果与官方交易状态，不是浏览器提交的数据。
- `backend/src/main/java/com/wwzyhy/shopagent/exception/AlipayQueryUnavailableException.java`：安全查询异常，不附带可能包含 SDK 原始材料的异常链。
- `backend/src/main/java/com/wwzyhy/shopagent/service/AlipayTradeQueryService.java`：使用已持久的 out_trade_no 与命名沙箱 SDK Client 查单；核对返回金额、商户交易号、官方交易号、状态与时间。
- `backend/src/main/java/com/wwzyhy/shopagent/service/AlipayPaymentSettlementService.java`：订单/支付/库存原子结算，重复及并发入账不会重复消费。
- `backend/src/main/java/com/wwzyhy/shopagent/service/AlipayPaymentRefreshService.java`：事务外查询、独立事务结算的协调；终态直接返回本地结果。

修改 3 个文件：`service/PaymentService.java` 增加按渠道分流和可选沙箱服务；`exception/PaymentExceptionHandler.java` 增加 503 `ALIPAY_QUERY_UNAVAILABLE`；`config/AlipaySandboxConfig.java` 拒绝空密钥材料，避免缺支付宝公钥时 SDK 没有响应验签器。

没有新增 Controller、数据表或迁移，也没有改 Python。原 MOCK 结算实现和只扫描 MOCK 的补偿任务保持当前职责；本组未启用官方自动查询任务。

## 状态及验证约定

- SDK 使用配置的支付宝公钥验证成功查询响应。查询响应不回传 app_id/seller_id；代码核对支付单保存身份与当前直接调用 Client 配置，并再次核对内部结果与锁定的支付单，不能宣称在查询响应里比较了不存在的字段。
- `TRADE_SUCCESS`、`TRADE_FINISHED`：核对通过后入账；官方交易号和 `send_pay_date` 保存为 provider 字段。本地订单 paid_at 为本地入账时刻。
- `WAIT_BUYER_PAY`、`ACQ.TRADE_NOT_EXIST`：保持 PENDING 和预占。链接可能尚未被打开，查不到交易不是永久关单证明。
- `TRADE_CLOSED` 既可能代表未付款关闭，也可能代表付后全额退款。本组保持 PENDING/预占，不由这一状态直接推断释放库存。关单与异常核对另组接线。
- 查询失败、错签/无签名、返回信息不符、未知状态或缺少成功付款时间：503，保持业务状态。缺少已付款时间时不编造时间入账，待重试或后续可信通知补全。
- 时间从 SDK 已验证的原始响应解析，使用上海时区后转换为业务 Clock 时区，避免依赖 SDK Date 的 JVM 默认时区；严格校验格式、明显过早/未来时间。
- 已确认成功不能仅因本地截止时间已过而丢弃；只要本地仍待支付且预占还在，照常原子入账。现有 guard 继续阻止未知官方结果被本地取消/过期提前释放。
- 终态刷新直接返回；过时的等待结果不能把已成功支付退回 PENDING。官方交易号唯一约束阻止同一笔官方交易支付两个本地订单。

协议来源：[官方查询说明](https://developer.alibaba.com/docs/api.htm?apiId=757&docType=4)、[官方 SDK 查询响应字段](https://github.com/alipay/alipay-sdk-java-all/blob/master/v2/src/main/java/com/alipay/api/response/AlipayTradeQueryResponse.java)、[SDK 响应验签](https://github.com/alipay/alipay-sdk-java-all/blob/master/v2/src/main/java/com/alipay/api/AbstractAlipayClient.java)、[SDK 公钥与验签器配置](https://github.com/alipay/alipay-sdk-java-all/blob/master/v2/src/main/java/com/alipay/api/DefaultAlipayClient.java)。公开 master 可能更新；本次实际执行版本为 Maven 固定的 4.40.1044.ALL。

## 验收证据

Java 17 / Maven `-DskipTests package` 成功，212 个 Java 源文件。没有把跳过 JUnit 的构建称为 JUnit 测试成功。

本次最终构建的两组检查共 **244/244 通过**：

1. [新查询与结算 107 项](verification/alipay-query-formal-2026-10-24.json)：正式编译类与打包 JAR 全部 class 字节一致；正式 Spring Web HTTP / MyBatis / 真实本地 MySQL，实际 SDK 签名和验签，仅替换 SDK 出站 HTTP 传输为受控模拟响应。30 次模拟 SDK 请求，没有向支付宝发出请求。
2. [原 MOCK 支付回归 137 项](verification/alipay-query-mock-regression-2026-10-24.json)：同一 JAR 的真实 HTTP、持久模拟回执、重启恢复、定时补偿、模拟关闭开关、竞争与回滚及已有接口。

新组覆盖本人权限与 CSRF、越权前零查询、错签/无签名/超时/官方错误、商户交易号与金额不符、缺失/未知状态、缺交易号/时间、时间异常、成功/完成状态、重复及并发刷新、本地超时后迟到成功、配置身份错配、内部结果七种错配、消费第一/第二 SKU 后及订单/支付 UPDATE 后和读回失败的真实 SQL 回滚、锁超时 409 与重试、查询等待期间取消快速安全拒绝、相同官方交易号重复入账全回滚、旧 RR 外层快照下独立刷新看到新结果、空公私钥拒绝及证书安全策略保持。

开发过程中第一轮发现 SDK 无签名响应可能抛 RuntimeException 而非 AlipayApiException，先前返回 500；已在查询边界转换为不带原异常链的安全 503，并用最终构建全组复验。修改前报告 `alipay-query-pre-fix-2026-10-24.json` 保留，不计入最终通过数。

两轮最终证据源与 JAR SHA256 一致，执行期间源码未变。每轮原有 12 表业务行摘要保持；测试账号/隐藏商品/终态订单支付留痕，地址移除，测试 PENDING 支付/待支付订单/库存预占均为 0，自建临时进程已停止。只有从未提交到支付宝的虚构身份测试单使用辅助 SQL 关闭后由正式订单服务释放，辅助清理不算官方关单实现。

## 尚未完成的部分

以上证明代码链和受控响应下的正确性，没有读取用户支付宝私钥/私密配置、调用真实沙箱 API、打开收银台或完成实际沙箱付款。官方自动补偿、关单、通知验签、公网回调、完整交易页面和 Linux 迁移另行完成。

本机 MySQL 是既有 8.0.34/shopagent；本组无 DDL/GRANT、无 Linux/VMware/模型/Git 操作。审计进程临时使用原生 Tomcat 会话，正式 Redis 会话配置未改，本组不当 Redis 重验。
