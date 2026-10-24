# 通知、补偿与交易页面：集中交付

2026-10-24，用户明确授权直接完成路线①支付通知与验签、②官方查单补偿、③完整交易页面，并要求三组完成后只做一次必要的集中验收。已按此范围实施；第四步真实沙箱联调留给用户手动操作，下一对话从第四步开始。

## 已交付

### ① 支付通知

- 新增匿名 `POST /api/payments/alipay/notify`，仅此精确 POST 路由免登录与 CSRF；其他写接口保留保护。
- 仅接受 UTF-8 的表单协议，限制 64KiB 正文、参数数量与长度，拒绝重复字段和 URL 查询串混入参数。
- 使用已配置 SDK Client 的支付宝公钥验签器，执行 RSA2 通知 V1 规则；检查 app_id、seller_id、可选 auth_app_id、商户交易号、金额、官方交易号、状态和付款时间。
- 复用已有独立结算事务，提交成功后才返回纯文本 `success`；拒绝或未提交返回纯文本 `failure`。重复、乱序通知不重复消费库存，也不把已成功支付退回等待。
- 查询的 send_pay_date 和通知的 gmt_payment 来源语义有差别，重复成功以同一官方交易号、身份、金额确认一致，保留第一次可信记录的付款时间，不要求两个不同来源时间逐毫秒相等。
- WAIT/CLOSED 不直接释放库存；不把单纯 CLOSED 当未付款关闭。异常终态仍需核对，不实现退款。

### ② 官方查单补偿

- 沙箱开关开启后，默认每30秒检查一批最多20张官方 PENDING 支付单，可配置开启/关闭、间隔与批量（1—100）。
- 轮转 ID 游标，避免一直失败的旧交易堵住后续交易；每张单独查询、独立结算，失败隔离。
- 网络查询不持有业务事务与行锁；每次重新读取未完成支付。日志只记录本地支付编号与异常类型，不打印原始通知、SDK异常链、买家信息或密钥。
- 查不到、等待付款保持原状态；模糊 CLOSED 提示需核对。原 MOCK 补偿任务职责不变。

### ③ 交易页面

延续“跳糖选物”的字体、蓝黄配色和现有商城导航，使用原生 HTML/CSS/JavaScript，无新增前端依赖。

- `addresses.html`：新增、修改、删除地址和设置默认；真实调用本人地址接口。
- `checkout.html`：选择地址与商品、服务端核价、显式提交订单；UUID 请求键按用户和下单内容保留，超时可用原请求重试。
- `orders.html`：本人订单列表、状态过滤、分页与详情入口。
- `order.html`：订单快照、金额、期限、支付记录、取消、准备/继续沙箱付款及后端状态查询；仅在对应开关启用时显示本地模拟支付操作。
- 购物车接入结算，既有页面增加订单/地址导航。小屏导航在容器内横向滚动，不撑宽整页；原有其他视觉方案及备份页保留。
- 新增本人 `GET /api/orders/{id}/payments`（最近20次）、`GET /api/payments/options` 和 `POST /api/payments/alipay/{id}/page`。继续付款由服务器读取原请求键，刷新页面/换浏览器无需获得他人的请求键或创建新交易。
- 付款页面参数不能决定 PAID；只读轮询读取后端状态，实际结算只走通知/可信查单。尚未点击真实外部收银台链接。
- 本轮下单后购物车保留，页面明确说明；没有在后台异步删除可能已被用户修改的购物车内容。

## 主要文件

后端新增7个 Java：

- `controller/AlipayNotifyController.java`、`controller/AlipayResumeController.java`、`controller/PaymentOptionsController.java`
- `service/AlipayNotifyService.java`、`service/AlipayPaymentLookupService.java`、`service/AlipayPaymentReconciliationService.java`
- `config/AlipayReconciliationConfig.java`

定时任务放在配置类内的静态类中，正式 Java 源数量为227。

修改：`config/SecurityConfig.java`、`service/AlipayPagePayService.java`、`service/AlipayPaymentSettlementService.java`、`service/PaymentReadService.java`、`controller/OrderController.java`、`exception/PaymentExceptionHandler.java`。

前端新增四个 HTML 与 `trade.js`、`trade.css`；修改共用 `store.js`、`store.css` 以及商城首页、列表、商品详情、购物车、资料问答导航。完整路径前缀分别为 `D:\ShopAgent-X\backend\src\main\java\com\wwzyhy\shopagent\` 和 `D:\ShopAgent-X\backend\src\main\resources\static\shopagent\`。

## 一次集中验收阶段

没有按①②③分别启动一套验收，也没有重跑原来的432项。三组均写完后统一编译、核关键接口、定时任务与浏览器流程。

- Java17/Maven `-DskipTests package` 成功；跳过 JUnit，不将打包命令当 JUnit 测试。
- 初次集中编译修正了 SDK SignChecker 不声明受检异常的 catch；首轮联合验收后端关键检查全部通过，浏览器发现390px新增导航导致溢出。
- 修复仅涉及共用导航 CSS；Java 源哈希未变。保留[修复前记录](verification/trade-completion-before-mobile-fix-2026-10-24.json)，复用已通过的37项后端/合同及前置检查，只对受影响的前端继续验收。
- [最终记录](verification/trade-completion-2026-10-24.json) accepted=true，包含22项最终结构/环境/清理检查和浏览器27项检查；不将重复前置检查累计成一套新的大测试数字。
- 验证了匿名有效回调无 Cookie/CSRF 入账、错签/错身份/错金额/错时间/重复参数/混合参数/正文限制、重复及乱序通知、第二 SKU 不足时第一 SKU 的真实消费 SQL 回滚、修复后同一通知重试、关闭后迟到成功拒绝、其他写接口仍需 CSRF、归属隔离、继续原支付与签名通知/返回 URL、轮转批量防阻塞，以及实际 Spring 定时任务补回漏通知付款。
- Edge 真浏览器通过：登录→添加默认地址→购物车→核价→下单→模拟失败重试→模拟成功→本人订单过滤；另验证伪造 URL 成功参数不能入账、刷新后继续同一沙箱支付、取消关单、删除地址后订单快照不变及退出后私密数据从页面清除。
- 四个新页面均检查1440/768/390宽度无整页横向溢出，保存截图并实际查看了桌面结算、手机订单、手机地址页面；浏览器无未捕获脚本异常、没有发出外部付款导航请求。

演示截图中的账号、地址和商品是合成测试数据，不是用户真实资料。

## 数据与边界

使用正式编译代码、真实本机 MySQL 和 SDK RSA2 签名/验签，但出站 HTTP 被测试传输接管，密钥与应用/商家身份临时生成。没有读取用户支付宝私密材料，没有实际官方支付/查单/关单或公网回调请求，没有调用模型或修改 Python。

原有12张业务表行摘要保持；测试地址与购物车移除，测试商品隐藏，终态订单/支付和测试账号留痕，测试待支付订单/PENDING支付/库存预占均为0。所有自建审计进程和定时上下文均已停止。仅审计进程使用原生 Tomcat 会话，正式 Redis 配置未变。没有 DDL/授权、Git、Linux、VMware、Docker 或公网隧道操作。

最终资源已重新打包并确认源/JAR相符；前端修正后的资源版本与首轮后端验收JAR资源不同，但Java源码相同，不能声称两轮JAR SHA完全相同。

## 第四步交接

先检查本机 MySQL/Redis 与 IDEA 启动配置，再由用户启动服务，浏览新页面。随后准备真正可访问的 HTTPS 通知地址，在本机配置中设置：

```properties
shopagent.alipay.sandbox.notify-url=https://实际测试域名/api/payments/alipay/notify
shopagent.alipay.sandbox.return-base-url=http://127.0.0.1:8888
shopagent.alipay.sandbox.reconcile.enabled=true
```

上面的域名是待用户配置的占位说明，当前没有创建公网地址；临时验收使用的 `callback.example.test` 也不是真实可访问地址。不要把它照搬为正式配置。

真实联调要分别验证通知和补偿：通知专测可先临时关闭自动补偿、付款后只读观察订单，让通知本身完成入账；再单独测试漏通知时启用补偿恢复。仅看到 PAID 不足以同时证明两条路径均已通过。接下来助手逐步教，用户手动操作；不自动代付、开公网隧道或部署Linux。

协议参考：[官方电脑网站支付通知](https://developer.alibaba.com/docs/doc.htm?articleId=105902&docType=1&treeId=193)、[官方SDK通知验签实现](https://github.com/alipay/alipay-sdk-java-all/blob/master/v2/src/main/java/com/alipay/api/internal/util/AlipaySignature.java)。实际依赖版本仍为固定4.40.1044.ALL。
