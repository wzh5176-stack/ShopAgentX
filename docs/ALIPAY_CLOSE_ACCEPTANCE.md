# 官方关单与订单取消、超时协调

2026-10-24。用户同意继续，并确认本组仍为 IDEA 中的 Java 开发。已直接完成本机代码；复用现有字段，无新增表、迁移、授权或 Python 修改。

## 实际流程

```text
POST /api/orders/{id}/cancel 或 OrderExpiryService
  → OrderCloseCoordinator
    → 先尝试原 OrderCloseService（本人归属/状态检查/短事务）
    → 有官方 PENDING 支付：事务回滚并转官方协调
      → 新事务读取本人支付快照，结束事务
      → 事务外官方查单
        → 已付款：原子入账，不释放库存；取消返回 409，超时任务不计过期
        → 待付款：事务外调用 alipay.trade.close，验证成功响应与交易号
        → 查不到：只有固定付款链接截止时间已过且另过 5 分钟，才允许结束
        → 结果不明确：保留 PENDING 与预占，返回可识别错误
      → 独立事务保存官方关闭结果：支付 CLOSED，记录官方交易号（如有）
      → 原本地订单关闭事务：订单 CANCELLED/EXPIRED + 库存释放一起提交
```

官方关闭结果先持久化。若下一步本地释放库存失败，支付仍 CLOSED、订单仍待处理、库存仍预占；后续取消或超时任务可以直接完成本地关闭，不重复请求支付宝。此间新官方或 MOCK 支付均受保护，不能再次创建。旧请求键只读回放已有支付，不生成新可付款交易。

这与 MOCK 原有的“本地关闭支付/订单/释放库存同事务”有所区别：真实外部系统不能参与本地数据库事务，故先可靠保存外部关闭结果，再重试本地关闭。网络等待期间不持有业务数据库事务或行锁。

## 文件变更

新增 8 个 Java 文件，均在 `backend/src/main/java/com/wwzyhy/shopagent/`：

| 文件 | 责任 |
| --- | --- |
| dto/AlipayCloseResult.java | 内部关单证明，区分 API 确认与链接到期后查无交易，不接受浏览器提交 |
| exception/AlipayCloseRequiredException.java | 原短事务发现官方待处理支付时，要求协调层先查单关单 |
| exception/AlipayCloseUnavailableException.java | 安全的关单失败提示，不携带 SDK 原始异常链 |
| service/AlipayCloseFence.java | 关闭结果已保存但本地关闭未完成时，阻止新支付 |
| service/AlipayTradeCloseService.java | 官方 SDK 关单、成功响应验签与返回交易号核对 |
| service/AlipayPaymentCloseService.java | 读取快照、校验并持久保存关闭确认；重查截止时间 |
| service/AlipayOrderCloseService.java | 查单、关单、付款抢先成功与不明确结果的协调 |
| service/OrderCloseCoordinator.java | 接入现有取消与自动超时入口，控制事务边界 |

修改 7 个文件：`controller/OrderController.java`、`service/OrderExpiryService.java` 改用协调层；`service/PendingAlipayGuard.java` 抛出专门的转交异常；`service/AlipayPaymentPrepareService.java`、`service/PaymentWriteService.java` 增加新支付保护；`service/AlipayPaymentSettlementService.java` 增加内部过期任务入账方法；`exception/OrderExceptionHandler.java` 映射查询/关单 503 及支付信息冲突 409。

`OrderCloseService` 的原有本地事务、锁顺序和库存释放实现未改；Controller 路径、USER/本人归属和 CSRF 要求未改。

## 真实接口规则及本项目边界

[官方关单接口](https://developer.alibaba.com/docs/api.htm?apiId=1058&docType=4)用于关闭已创建且未付款的交易。状态不合法、交易不存在等错误不能当关单成功。代码在相关竞争错误后重新查单；若发现付款成功，先完成订单入账，再拒绝取消。

SDK 成功响应按配置的支付宝公钥验证；本项目同时要求回传商户交易号、官方交易号与刚才可信查询一致。官方文档将这两个回传字段列为选填，本项目保守要求它们齐全，否则按 503 处理，库存不释放。

单纯 `TRADE_CLOSED` 仍不能证明是未付款关闭，因为[查询状态](https://developer.alibaba.com/docs/api.htm?apiId=757&docType=4)也可能表示付后全额退款。没有已保存的本地确认时保留库存，需核对，不能伪造成功关闭证据。

“查无交易”路径依赖以下边界：所有已签付款链接使用支付单不可延长的绝对截止时间；本机业务时钟须正确同步。截止前与截止后 5 分钟内都不释放。超过此时间再次主动查询确实不存在，才保存关闭并释放。本轮只用合成截止时间测试这些分支，未验证真实网关时钟或真实链接的过期行为。

官方关闭请求已生效但响应丢失、或确认无法落库时，后续可能只查到含义不明确的 CLOSED。本组按 409/503 保守保留库存，交由核对；不能宣称所有外部异常均能自动恢复。已保存关闭确认后发生的本地失败，则已验证可自动重试。

## 验收范围

Java 17 / Maven `-DskipTests package` 编译打包成功，220 个 Java 源文件；此命令跳过 JUnit，不将其当 JUnit 全套测试。

本组[关单专项结果](verification/alipay-close-formal-2026-10-24.json)的 **83/83 项通过**，正式编译类与打包 JAR 中 class 字节完全一致。使用正式 Spring Web / MyBatis / 真实本机 MySQL；实际 SDK 生成请求和验证响应签名，出站 HTTP 由有状态的测试传输接管。临时生成双 RSA 密钥，虚构应用/商家；没有读取用户支付宝私密材料、向官方发请求或完成实际付款。

覆盖：本人权限与 CSRF、拒绝请求前无 SDK 调用；正常/重复/并发取消、已付款与付款抢先成功、临近到期与缓冲、到期后查无交易、实际 WAIT 交易关闭、模糊 CLOSED、查询超时/关单超时/官方错误/错签/无签名/错误交易号、响应丢失重试不伪造确认、真实保存确认 SQL 后故障回滚、确认后第一/第二 SKU 释放及状态更新/读回故障回滚、期间阻止新官方/MOCK 支付、本地重试零额外 RPC、网络等待时并发刷新可入账、超时任务发现已支付以及批量失败隔离。

最初预检辅助脚本读错嵌套的 `original_overdue` 路径，已在创建任何测试账号/商品/订单前停止；报告 `alipay-close-preflight-2026-10-24.json` 保留，不计入正式通过数。只修正临时审计脚本路径，本组业务代码没有因该预检改动。

此前“网络等待期间取消只能触发 guard”的旧期望已被本组真实协调取代。查单回归移除该旧场景的 3 条检查，由本组有状态的“关单等待期间付款成功”竞争场景验证；其余查单检查保留。

## 后续

下一接通知验签及可重放、幂等的回调处理，再补完整交易页面并做实际沙箱付款。当前尚未验证真实账户的收银台、官方查单/关单、通知可达性或新版本 Linux 部署。

本组无 Linux/VMware、模型、Git、DDL/授权操作。审计进程使用临时原生 Tomcat 会话，正式 Redis 配置不变。

## 最终同构建回归

[汇总结果](verification/alipay-close-summary-2026-10-24.json)：共 **432/432** 项通过，正式源与JAR指纹一致且执行期间未变。

- 关单专项：83 项。
- [查询及成功结算回归](verification/alipay-close-query-regression-2026-10-24.json)：104 项，旧网络等待 guard 的 3 条由新关单竞争检查取代。
- [MOCK 支付完整回归](verification/alipay-close-mock-regression-2026-10-24.json)：137 项，包含实际打包 JAR HTTP、重启、定时补偿和开关。
- [订单生命周期回归](verification/alipay-close-lifecycle-regression-2026-10-24.json)：108 项，包含实际打包 JAR 定时过期、分页/取消、库存与初始化回滚。

关单/查询/MOCK各轮原有12表数据行摘要一致；生命周期轮验证其原有10表摘要。每轮仅自建测试账号/隐藏商品/终态订单支付留痕、测试地址移除，测试待支付/预占清零；支付相关轮的测试PENDING支付也为0。自建临时服务与工作进程全部停止。辅助关闭清理只作用于虚构身份、从未提交到支付宝的测试单，不算实际官方关单证据。
