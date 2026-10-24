# 订单列表、取消和自动超时验收

后续更新：阶段11本地模拟支付后端也已完成，见[模拟支付验收](MOCK_PAYMENT_ACCEPTANCE.md)。下文“下一手写模拟支付”是本报告原时点的安排，当前下一入口为官方沙箱准备。

日期：2026-10-24。用户授权“这一组也直接帮我搞，模拟支付再手动开始”。本组直接完成阶段10剩余后端，下一阶段11恢复手写教学。

## 结果与范围

正式Java17/Maven package成功，174个Java源文件参与编译；构建使用skipTests，不称执行JUnit。当前构建的真实HTTP、正式Service/MyBatis与Windows MySQL检查：新增生命周期108/108通过，原建单回归96/96通过。两份报告各自记录源/JAR指纹，测试期间源码未变：

- [生命周期108项](verification/order-lifecycle-formal-2026-10-24.json)
- [当前构建建单回归96项](verification/order-lifecycle-create-regression-2026-10-24.json)

阶段10本机后端完成；尚无模拟支付、官方沙箱、交易页面及本轮Linux更新。PAID只是测试桥对新合成订单构造的终态样本，不代表实现了支付接口。

## 新建与修改的正式文件

以下路径均相对项目D:\ShopAgent-X；链接可直接打开文件。

| 文件 | 用途 |
| --- | --- |
| [OrderSummaryResponse.java](../backend/src/main/java/com/wwzyhy/shopagent/dto/OrderSummaryResponse.java)（新） | 列表每条订单的编号、状态、总额与时间摘要 |
| [OrderPageResponse.java](../backend/src/main/java/com/wwzyhy/shopagent/dto/OrderPageResponse.java)（新） | items及page/size/total/pages |
| [OrderReadService.java](../backend/src/main/java/com/wwzyhy/shopagent/service/OrderReadService.java) | 本人订单分页、可选状态筛选，只查询摘要列 |
| [OrderController.java](../backend/src/main/java/com/wwzyhy/shopagent/controller/OrderController.java) | GET /api/orders与POST /api/orders/{id}/cancel |
| [SecurityConfig.java](../backend/src/main/java/com/wwzyhy/shopagent/config/SecurityConfig.java) | 新路由要求USER，取消保留CSRF |
| [ProductInventoryMapper.java](../backend/src/main/java/com/wwzyhy/shopagent/mapper/ProductInventoryMapper.java) | 原子释放预占；预占和释放都检查数量与整数上限 |
| [OrderCloseService.java](../backend/src/main/java/com/wwzyhy/shopagent/service/OrderCloseService.java)（新） | 同事务锁订单、按商品编号顺序释放库存、记录关闭状态 |
| [OrderExpiryService.java](../backend/src/main/java/com/wwzyhy/shopagent/service/OrderExpiryService.java)（新） | 扫描到期候选，每单独立事务，单笔异常不回滚其他订单 |
| [OrderExpiryJob.java](../backend/src/main/java/com/wwzyhy/shopagent/service/OrderExpiryJob.java)（新） | 定时触发扫描 |
| [OrderSchedulingConfig.java](../backend/src/main/java/com/wwzyhy/shopagent/config/OrderSchedulingConfig.java)（新） | 开启可配置的Spring定时任务 |
| [ProductService.java](../backend/src/main/java/com/wwzyhy/shopagent/service/ProductService.java) | 创建商品与可售0/预占0的库存行同事务保存 |

没有新增SQL迁移、执行DDL/GRANT或扩大权限，没有自动Git提交推送。

## 调用路径和重要代码

### 1. 查看本人订单

`GET /api/orders?page=1&size=10&status=PENDING_PAYMENT` → OrderController.list → OrderReadService.list → MyBatis-Plus分页查询。

user_id来自登录身份；status可省略，page从1开始，size为1—50。按id倒序，响应为data内的items/page/size/total/pages；每项只有id/status/totalAmount/expiresAt/createdAt，不循环读取每单明细。详情继续用原GET /api/orders/{id}。

### 2. 主动取消与释放一次

`POST /api/orders/{id}/cancel` → OrderCloseService.cancel → `SELECT ... FOR UPDATE`锁本人订单 → 读取明细 → release → 更新订单 → 事务提交 → 返回详情。

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public OrderDetailResponse cancel(Authentication authentication, Long id)
```

这个事务把库存释放与订单关闭绑在一起。第二种商品释放失败时，第一种商品已经执行的释放也必须回滚。独立事务还避免调用方旧RR快照导致错误回放。

```sql
UPDATE product_inventory
SET available_quantity = available_quantity + ?,
    reserved_quantity = reserved_quantity - ?
WHERE product_id = ? AND reserved_quantity >= ?;
```

上面是核心含义示意；正式SQL还检查数量1—99与整数上限。例：可售8、预占2，取消2件后变为可售10、预占0。受影响行数必须为1，否则整单回滚，不能凭空补库存。

未到截止时间的待支付单变CANCELLED，已到期的变EXPIRED。已关闭订单重复取消返回原详情；PAID返回409。订单行锁使两个取消请求，或取消与过期任务同时到达时，后来的事务看到终态后跳过，库存只释放一次。商品后来下架/逻辑删除也不妨碍按购买明细释放预占。

### 3. 无人操作也能过期

Spring定时任务 → OrderExpiryService扫描到期PENDING_PAYMENT编号 → 通过另一个Spring Bean的事务代理调用OrderCloseService.expire。

默认启动30秒后首次执行，之后每次完成再等30秒，每批最多100单。可以用shopagent.orders.expiry.enabled=false关闭；delay-ms和initial-delay-ms可调整。GET查询本身不改订单，因此刚到截止时间时列表仍可能短暂显示待支付，任务会随后处理，不承诺精确在第15分钟瞬间关闭。

两个Java实例都可以扫描，由数据库订单锁和状态复核避免重复释放。本轮启动两个独立临时JAR实例，把任务周期设200毫秒，实际等后台任务自动关闭新合成过期单，并再次扫描确认没有重复释放；200毫秒仅为测试配置，正式默认仍30秒。

一笔库存异常会记录错误并保留该单，下一轮重试，其他候选继续；需人工排查数据不一致。连续大量异常占满最早100条可能阻挡后续候选，本轮小规模演示未实现死信隔离或生产任务平台。

### 4. 新商品不漏建库存

管理员原POST商品入口 → ProductService.create事务 → INSERT商品 → INSERT库存(0,0)。任一步失败都回滚；初始化0避免凭空假设可售数量。补货管理接口尚未实现。

## 实际验收覆盖

108项包含分页/状态筛选/归属/输入；匿名、ADMIN单角色和CSRF保护；重复取消、过期取消、PAID拒绝；取消四处/过期三处真实SQL后故障与整单回滚；库存不一致时回滚；批次单笔失败隔离与修复后重试；两个取消、取消与过期、两个过期事务竞争；旧RR调用方回放；真实库存锁超时409与重试；下架删除后释放；商品库存初始化三处失败回滚；双独立定时实例自动过期及六个原有路由回归。故障通过委托真实Mapper执行SQL后注入；没有外层测试事务自动回滚冒充业务事务成功。

96项回归针对同一当前构建，重验预览只读、真实核价、幂等快照、归属/CSRF、缺库存/第二SKU不足、创建故障回滚、锁超时、RR和并发最后一件等。历史首轮报告仍保留，不把不同构建检查当成同一轮。

## 数据和环境留痕

- Windows本地MySQL8.0.34/shopagent，应用身份shopagent_app@127.0.0.1；原有10表行内容在每轮前后哈希均不变，自增序号会前进。
- 生命周期轮新账号24/25，商品13/14/15/16已隐藏下架，地址2条已移除。保留14个测试订单：35/36/37/38/39/40/41/42/43/44/45/46/47/49；包含受控PAID样本，其余已取消/过期，测试待支付和预占均0。清理走本轮正式关闭Service；不扩大DELETE权限清除订单留痕。
- 建单回归轮另有专用账号/商品/订单，具体ID见回归JSON的final。该旧测试程序的收尾仍用受控辅助SQL关闭新合成订单，不用它冒充正式取消验收；正式取消验收来自108项生命周期轮。
- 本轮只对临时测试进程排除Redis Session自动配置，使用原生Cookie会话；正式配置不变。未复验Linux/Redis共享会话/Nginx。
- 两轮各自的临时主服务/桥进程及本轮两个定时实例已停止。未调用模型、读取打印API密钥或改变用户IDE/VMware/eNSP服务。

## 下一教学入口

阶段11本地模拟支付由用户手写：先设计独立支付单及状态，再建表/实体/Mapper；接着创建支付尝试、服务端模拟结果、同事务扣减预占并标记订单PAID，最后处理重复结果与取消/过期竞争、失败后重试和查单补偿，成组验收。支付不会再减少已经预占过的可售库存。

目前停在此入口，尚未创建支付表或支付业务代码。官方沙箱、交易页面和Linux迁移属于后续阶段12。
