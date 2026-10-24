# 结算预览、创建订单与详情接口验收

后续更新：本报告记录的是最小建单链当时的范围；列表/取消/超时已另验，阶段11本地模拟支付亦完成，当前见[模拟支付验收](MOCK_PAYMENT_ACCEPTANCE.md)，下一为官方沙箱准备。下文“尚未实现/下一手写”为历史，不覆盖最新状态。

日期：2026-10-24。用户明确授权本组由助手补齐HTTP接线并直接验收，下一组恢复手写教学。本次新增OrderController、OrderExceptionHandler，SecurityConfig追加两个精确USER规则；其余用户手写的请求/响应/准备/读写协调代码原样参与正式构建与测试。

## 结论

本机最小建单链验收通过：POST /api/checkout/preview、POST /api/orders、GET /api/orders/{id}。正式Java17/Maven package成功；168个正式Java源文件参与构建，构建使用skipTests，不能称执行了JUnit。正式打包JAR真实HTTP＋同构建正式Service/MyBatis真实MySQL事务测试共96项，全部通过。

机器证据：[order-create-formal-2026-10-24.json](verification/order-create-formal-2026-10-24.json)。记录正式源码哈希与JAR哈希，测试运行期间源码未变化。

阶段10仍未全部完成：本人订单分页列表、正式取消接口、过期任务/释放、新增商品初始化库存尚待下一组；模拟支付/官方沙箱/交易页面/本次Linux部署均未实现或更新。本报告不把实验清理SQL当作取消功能验收。

## 本次正式改动

| 文件 | 行为 |
| --- | --- |
| [OrderController](../backend/src/main/java/com/wwzyhy/shopagent/controller/OrderController.java) | 创建首次201、同内容回放200，都返回Location与data；GET详情复用本人归属检查 |
| [OrderExceptionHandler](../backend/src/main/java/com/wwzyhy/shopagent/exception/OrderExceptionHandler.java) | 仅作用OrderController，订单/地址404、不可售409、同键异内容及锁竞争409；保留全局参数/数据库等含义 |
| [SecurityConfig](../backend/src/main/java/com/wwzyhy/shopagent/config/SecurityConfig.java) | 仅追加POST /api/orders、GET /api/orders/*要求USER，CSRF和其他规则保留 |

没有自动写后续取消/分页/过期或支付模块，没有Git提交推送。

## 实际验证

1. **真实金额与预览只读**：后端读取合成商品299.00单价，2件合计598.00；客户端额外金额不能改变结果。预览前后订单/明细/库存完全不变。
2. **创建和详情**：首次201，Location可GET且数据一致；状态PENDING_PAYMENT、截止时间正好创建后15分钟，paidAt/closedAt为空；库存可售10→8、预占0→2。响应没有userId/requestKey/requestHash。
3. **幂等与快照**：同键同内容200，同ID同详情，库存不再变化；同键换数量/地址409 ORDER_CONFLICTED。同键可供不同用户各自创建独立订单。多商品调换提交顺序仍回放。商品改价/改名/下架、原地址修改/删除之后，历史详情及原请求重试仍返回原快照。
4. **安全和输入**：匿名401、ADMIN单角色403；缺失/串用CSRF403；他人订单404 ORDER_NOT_FOUND，他人地址404 ADDRESS_NOT_FOUND；非法编号/UUID/数量/重复商品/空项/超20种/错误JSON均400。缺库存行、第二种商品不足、0元总单均409且不留下写入。
5. **真实数据库回滚**：第二种商品不足时，已插主单、第一种预占和明细全部撤销。另在正式OrderWriteService事务代理上，用Mapper委托执行真实SQL后，分别在主单INSERT、库存reserve、明细INSERT后注入异常，以及写后回查返回空；四种场景均验证注入确实到达且抛出，事务外订单/明细/库存的已提交状态哈希不变。未使用外层测试事务的自动回滚冒充业务事务回滚。
6. **锁等待与新快照**：真实持有合成库存行锁，正式HTTP创建锁超时返回409 ORDER_CONFLICTED，先插的主单也回滚；独立ReadService能在调用方已有旧RR快照时看到另一事务新提交的同键订单。
7. **真实并发请求**：同键同内容两请求得到200/201，最终一单且只预占一次；同键不同内容得到201/409，只有胜者数量被占；两个用户抢最后1件得到201/409，最后可售0、预占1，没有超卖。这是小规模正确性测试，不是吞吐或高可用证明。
8. **既有路由回归**：健康、商品、身份、购物车、会话列表、地址列表正常；不调用Python或付费模型。

## 环境、数据和清理边界

- 实际MySQL8.0.34/shopagent，应用身份shopagent_app@127.0.0.1。无DDL或GRANT，无权限扩大；此前021/022缺失恢复记录见[SQL恢复记录](ORDER_SQL_RECOVERY.md)。
- 本地Redis未运行；仅本次两个临时测试进程排除Redis Session自动配置，使用Tomcat原生会话验证真实注册/登录/Cookie/CSRF。正式application.yml和管理员配置不变；不重复宣称本轮验收了共享Redis登录、双Java、Nginx或Linux部署。
- 测试只使用新建专用账号id20、21，合成商品id7、8、9；其中商品9故意不建库存行测试缺行。测试期间未更新原有商品或库存。最终10张表的原有业务行内容哈希比较均不变；自增计数会因插入/回滚而前进，不属于行内容比较。
- 保留7个测试订单（id1、4、5、11、12、15、16）及相应明细，全部由**测试辅助清理事务**释放库存并标成CANCELLED；这不是尚未编写的正式取消Service。测试预占与待支付订单均为0，测试地址已删除，三个合成商品逻辑删除并下架，两个随机密码测试账号保留。未增加DELETE权限去物理删除留痕记录。
- 两个自建临时Java进程已停止，原MySQL服务继续运行。没有启动/停止用户IDE服务、Python、VMware、eNSP或Docker Desktop；未读取/打印API Key、数据库密码、私钥或Cookie，没有模型调用。

## 给用户的本组说明

浏览器提交编号和数量后，OrderCreateService先查同请求旧单；需要新建时，独立OrderWriteService事务重新读取真实地址/在售商品并核价，再保存订单、预占库存、保存明细。写事务代理提交后，Controller才返回201；重试同请求返回200和同一订单。GET详情按登录用户归属加载购买快照。

接下来回手写教学：本人订单列表、取消与超时释放库存。接齐这些之后再做对应整组验收，再进入模拟支付阶段；不在本组后直接跳沙箱。
