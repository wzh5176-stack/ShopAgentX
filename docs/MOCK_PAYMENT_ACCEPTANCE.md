# 阶段11：本地模拟支付验收

日期2026-10-24。用户明确“这些都直接帮我搞，什么时候到沙箱”，本组覆盖此前本地模拟部分的手写约定。助手直接实现并独立验收后端，下一入口为官方沙箱准备，不再追加普通CRUD准备组。

## 本机结果

正式Java17/Maven package成功，200个Java源文件参与构建，使用skipTests，不称执行JUnit。最终构建真实HTTP＋正式Service/MyBatis＋Windows MySQL检查：

| 证据 | 通过数 | 范围 |
| --- | --- | --- |
| [支付主流程](verification/payment-formal-2026-10-24.json) | 137/137 | 创建/读/模拟结果/补偿/重启/定时/权限/回滚与竞争 |
| [支付补充检查](verification/payment-races-2026-10-24.json) | 28/28 | 同键并发、真实超时事务竞争、旧RR下查新回执并结算、失败取消竞争、下架后付款 |
| [当前构建建单回归](verification/payment-create-regression-2026-10-24.json) | 96/96 | 原预览、建单、幂等、快照、库存竞争和回滚 |
| [当前构建订单生命周期回归](verification/payment-lifecycle-regression-2026-10-24.json) | 108/108 | 原列表、取消、自动超时、商品库存初始化 |

总计369项检查，包含环境和原数据/清理检查，不能解释为369个不同业务场景或JUnit测试。四份最终报告应为同一JAR/源指纹；机器汇总见verification/payment-acceptance-summary-2026-10-24.json。

这是本地模拟**后端**完成，现有页面尚无交易结算/支付按钮。未接官方沙箱、未用真实资金、未更新Linux/Redis/Nginx部署。

## 正式实现

### 数据与HTTP

- 复用用户已手写的PaymentAttempt、PaymentAttemptMapper、PaymentStatus、PaymentCreateRequest/Response/CreateResult及023/024 SQL。
- POST /api/payments：本人订单创建支付单，首次201/原键回放200，Location指向详情。金额和截止时间取订单，忽略客户端自报金额/状态/渠道。同单新键遇到已有待处理/成功记录409，避免不同键复用产生不清晰的重试语义；覆盖旧基础教案中的“不同键复用”候选。
- GET /api/payments/{id}：本人支付详情，只读。
- POST /api/payments/{id}/mock-result：仅MOCK开关开启才注册，服务端记录SUCCEEDED或FAILED；同结果重复回放，不同结果不能改写。
- POST /api/payments/{id}/refresh：查询已保存渠道结果，再做商户结算；状态修改使用POST和CSRF，GET不偷偷结算。
- 所有入口要求USER与本人归属，POST沿用CSRF。专用PaymentExceptionHandler仅作用两个支付Controller；全局新增明确的资源404处理，避免关闭模拟入口后被通用Exception处理成500。

### 三个事务边界

1. PaymentWriteService：锁本人订单→查原键→校验待支付/到期→检查现有待处理/成功→保存支付单。旧键重试优先回放原记录，失败后新尝试换键。
2. MockPaymentGateway.record：在独立事务写不可变模拟回执mock_payment_receipts，金额来自支付记录。回执跨Java重启保留。数据库应用账号只有此表SELECT/INSERT，没有UPDATE/DELETE。
3. PaymentService在商户结算事务外查渠道；MockPaymentGateway.query使用独立只读事务，避免调用方旧RR快照遮住新回执。PaymentSettlementService再独立锁订单→锁支付→按商品id消耗预占→订单PAID→支付SUCCEEDED，全部一起提交。任何一步失败完整回滚，已提交回执仍可补偿。

成功只减少reserved_quantity，不再次减少available_quantity。失败只结束本次尝试，订单继续PENDING_PAYMENT并保留预占；取消/超时在同一事务关闭PENDING支付、释放预占并关闭订单，历史FAILED不改。并发操作统一先锁订单；重复终态不再扣/放库存。

OrderCloseService新增MANDATORY的同事务过期方法，供支付查单在已经持有订单锁的当前事务使用，避免嵌套REQUIRES_NEW反等自己持有的订单锁。外部原expire仍是独立事务。

### 自动查单

启用MOCK后默认开启PaymentReconciliationJob，每次完成后间隔30秒、启动30秒后首次，扫描最多100个PENDING逐单查询/独立结算；单笔失败记录日志后继续，不回滚其他单。shopagent.payments.reconcile.enabled=false可暂停。自动订单过期仍使用原任务，二者靠订单锁和终态复核协调。

测试使用200毫秒周期启动一个独立JAR，实际等任务结算已保存回执；另实际重启商户JAR，再查询补偿成功。不是直接调用方法冒充自动执行/持久化恢复。

大量最早待处理或异常记录可能占满100条扫描窗口，本轮未实现任务分片、死信隔离、生产监控或吞吐保障。

## 关键实际验证

- 服务端金额598.00，客户端自报0.01/其他渠道/成功不能改变结果；创建支付不会再次预占。
- 同键回放、旧失败键回放、失败后新键重试、成功/关闭订单禁止新付款；同键并发201/200一条记录，不同键并发201/409最多一条待处理。
- 同结果并发均200且仅消费一次；矛盾结果一胜一409；付款与取消、付款与实际expire事务竞争，最终只有一个合法订单终态，库存不重复消费/释放。失败与取消竞争也验证。
- 渠道金额不匹配拒绝且数据不变；另一用户/匿名/ADMIN单角色及缺失串用CSRF被拒；非法UUID、编号、枚举均400。
- 真实SQL后故障：创建INSERT/回查、回执INSERT、结算第一/第二SKU消费、订单更新、支付更新、回查；取消释放失败连同支付关闭一起回滚。都确认注入点到达、异常抛出，事务外已提交状态比较不变，没有外层测试事务自动回滚替代业务回滚。
- 真实库存行锁超时409，回执保留，订单/支付/库存不变，解除锁后refresh成功。
- 旧RR调用方读到PENDING后，另一事务只提交模拟成功回执；调用方内部refresh能独立查到新回执并结算SUCCEEDED，而外层旧快照仍PENDING。
- 一笔故意不一致库存不会影响同批另一笔结算，修复仅测试行后下轮可恢复。商品后来下架/逻辑删除，不阻止已有合法订单按购买快照付款。
- 关闭模拟开关后模拟路由404、新支付503，不能继续手动指定成功。

## 数据库与收尾

真实Windows127.0.0.1:3306/shopagent，MySQL8.0.34。023表原已存在（0行），没有重建；实际新增025回执表并执行024/026精确授权，记录见[payment-schema](verification/payment-schema-2026-10-24.json)。无其他表DDL，没有扩大应用对旧表权限，没有改账号密码。

只使用新专用账号、新合成商品与订单。最终支付主/补充轮分别对原12表行内容做前后哈希比较；两轮订单回归比较原10表。测试地址删除、合成商品隐藏下架；测试订单/支付保留成功、失败、取消或过期留痕，各轮最终预占/待支付订单/PENDING支付为0。完整ID见各JSON的final及汇总；未增加DELETE权限清除留痕。自增序号会因插入/回滚而前进。

首次支付预检因测试辅助程序只捕获IllegalStateException，而注入回查空实际抛PaymentNotFoundException而中止，报告payment-preflight-2026-10-24.json保留；84项已通过但整轮不完整，不能加进最终通过数。修正辅助捕获后完成r1；之后发现旧RR调用方可能遮住模拟查询结果，给模拟query补独立只读事务并加强该专项，再对最终构建重跑上表四轮。r1文件保留，不混入369项。

只在临时测试进程排除Redis Session自动配置，使用原生Cookie验证登录/CSRF；正式配置未改，不宣称本轮通过Redis共享会话或Linux集成。所有自建临时JAR/测试桥/定时实例停止；MySQL保留运行。不调用Python/模型，不打印私钥、密码或Cookie，没有Git提交推送、VMware/eNSP/Windows虚拟化操作。

## 本地模拟边界与下一步

本地以商户关闭/截止为准，迟到模拟成功不能重新打开CANCELLED/EXPIRED订单；模拟回执可保留成功而商户已关闭，这是有意覆盖的竞争窗口，没有真实扣款。不能据此认为官方渠道付款后可以直接忽略结果。阶段12另接官方查询/通知验签/关单与异常对账规则，再与商户事务衔接。

开关和四个API使用见[本地调用说明](MOCK_PAYMENT_USAGE.md)。下一步直接[登录官方沙箱确认应用及产品](ALIPAY_SANDBOX_PREPARATION.md)，尚未生成/配置沙箱密钥或发起官方支付。官方沙箱和完整交易页面、Linux迁移仍未完成。
