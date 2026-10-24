# 本地模拟支付：运行与调用

阶段11后端，仅用于本机模拟，没有真实资金，也没有接支付宝沙箱。阶段12才接官方渠道与交易页面。实现/验收结果见MOCK_PAYMENT_ACCEPTANCE.md。

## 开关

IDEA原来的Java启动配置中添加环境变量，保留已有变量：

```text
SHOPAGENT_PAYMENTS_MOCK_ENABLED=true
```

默认false，不注册模拟结果Controller，也不允许创建新的MOCK支付单。读历史和刷新已有支付单仍可用。不要为支付关闭原来的Session/CSRF/用户保护。本轮临时验收排除Redis只用于测试进程，不修改正式配置；平时仍按原开发方式启动依赖。

启用MOCK时，默认同时开启支付查单任务：启动30秒后首次执行，每次完成后等30秒再执行，每批最多100个PENDING支付单。临时需要停自动补偿可加：

```text
SHOPAGENT_PAYMENTS_RECONCILE_ENABLED=false
```

调试周期可用shopagent.payments.reconcile.delay-ms/initial-delay-ms。任务启停不会清除数据库记录，恢复后仍可查询已保存的模拟结果。大量最早待处理/异常记录可能占满100条候选，当前演示未实现任务分片/游标或死信隔离。

## 五个调用步骤

全部使用普通USER登录会话；POST携带同一会话对应的CSRF请求头。沿用项目已有GET /api/csrf获取token/headerName的方法。ADMIN单角色不允许操作本人交易。下面id/时间/金额都是演示，实际用自己的订单和接口返回编号。

1. 用原POST /api/orders创建本人待支付订单。购买金额由订单后端核算，创建时已经预占库存。
2. POST /api/payments创建支付尝试：

```json
{
  "orderId": 100,
  "requestKey": "b6209e80-970f-4f62-a810-72d4be7aefb6"
}
```

首次201、重复同键200，返回Location: /api/payments/{id}，正文data为PaymentResponse。订单+请求键唯一；同键即使后来失败/成功/关闭仍返回那次原记录。新键遇到已有PENDING/SUCCEEDED则409，避免让不同请求键含糊地指向同一次尝试。此前基础教案说“不同键复用当前尝试”，正式实现以这里的409规则为准。失败后用新键才能新建一次付款。

3. GET /api/payments/{id}只读查看：data包含id/orderId/channel/status/amount/expiresAt/finishedAt/createdAt/updatedAt。金额两位字符串，无requestKey或用户编号。支付金额取订单total_amount，截止时间取订单expires_at。客户端额外提交amount/status/channel不能改变它们。
4. POST /api/payments/{id}/mock-result指定本地模拟渠道结果：

```json
{ "outcome": "SUCCEEDED" }
```

或：

```json
{ "outcome": "FAILED" }
```

服务端先在独立事务中保存不可改写的模拟回执，再查询这份回执，随后在商户独立事务更新支付/订单/库存。相同结果重复提交200回放，不同结果试图改写409。关闭MOCK开关后此路由不注册，返回404；不是无保护的通用支付通知入口。

5. POST /api/payments/{id}/refresh主动查单补偿，不需要正文。后端从渠道适配器读取结果，不能把浏览器传来的任意status当可信结果。GET本身不执行结算；没有回执且未过期时refresh仍返回PENDING，有回执则尝试结算，后台任务也复用这套逻辑。

## 成功/失败如何影响库存

假设原可售10、预占0，下单2件：可售8、预占2。

| 操作结果 | 支付单 | 订单 | 可售/预占 |
| --- | --- | --- | --- |
| 创建支付尝试 | PENDING | PENDING_PAYMENT | 8/2 |
| 本次失败 | FAILED | PENDING_PAYMENT | 8/2 |
| 新尝试成功 | SUCCEEDED | PAID | 8/0 |
| 未付款取消/过期 | 当前PENDING变CLOSED | CANCELLED/EXPIRED | 10/0 |

成功只消耗预占，不再次减少可售；失败不释放订单预占，因为订单还允许重试。取消/过期同事务关闭所有当前PENDING支付并释放库存；历史FAILED保持原样。

支付、取消、超时统一先锁订单，再锁支付行，再按商品编号顺序操作库存。订单/支付/预占修改一起提交，任一步失败整体回滚；结果回执是先提交的另一事务，因此商户失败后可用refresh或任务重试，不丢结果。

模拟回执与订单状态不是同一回事。本地模式以商户关闭/截止为准：已取消/过期单不会因迟到模拟成功重新PAID；回执保留用于说明竞争窗口。这里没有真实扣款。阶段12必须另外实现官方查询/通知验签/关单与异常对账规则，不能把本地截止策略原样宣称为真实渠道处理保证。

## 错误含义

- 非法编号、UUID或结果枚举：400 INVALID_ARGUMENT。
- 匿名401；仅ADMIN、缺少/串用CSRF为403。
- 不存在/他人支付单：404 PAYMENT_NOT_FOUND；创建时不存在/他人订单：404 ORDER_NOT_FOUND。
- 关闭/已支付/超时订单、新键遇到现有待处理、改写模拟结果、锁竞争：409 PAYMENT_CONFLICTED。
- 模拟关闭时创建新支付：503 PAYMENT_UNAVAILABLE。
- 库存预占不一致或渠道返回金额/关联不匹配：内部错误并回滚，不凭空修补库存。

所有正式路由都是后端API；当前商城页面还没有结算/支付按钮，这部分随阶段12接完整交易页面。
