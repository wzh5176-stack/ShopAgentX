# 地址模块集中验收：2026-10-23

结论：用户手写的阶段9地址后端已通过本机验收，可以进入阶段10订单与库存。这里验收的是Java接口，不是地址网页或Linux新版部署。

## 实际执行

- 使用本项目Java17构建正式源码，Maven package成功。构建使用skipTests，不能称执行了JUnit套件。
- 正式打包JAR通过真实HTTP测试；另用同一次构建的正式AddressService、Mapper和真实MySQL测试事务故障及RR快照。辅助类只在忽略目录tmp/address-formal-audit，不修改正式业务代码。
- Windows本机MySQL实际8.0.34，数据库shopagent，连接身份shopagent_app@127.0.0.1。user_addresses存在，含外键、默认唯一生成列和两个启用的CHECK；该表权限恰为SELECT/INSERT/UPDATE/DELETE。
- 完整一轮111项检查全部通过，机器结果：[address-formal-2026-10-23.json](verification/address-formal-2026-10-23.json)。正式源码的文件哈希在运行前后相同。

## 覆盖的行为

1. 六个地址接口：新增201和Location、列表/详情、全量修改、设默认、删除204；首条默认、后续非默认、排序、同值修改、重复设默认、删除默认接替和最后一条删除。
2. 输入校验：空白、长度、电话格式和非字符串、缺少字段、错误JSON和非法路径编号；Service直接调用也校验。
3. 安全：匿名401、仅ADMIN角色403、写操作缺失/串用CSRF均403；另一普通用户读/改/设默认/删除均404 ADDRESS_NOT_FOUND，且原记录不变。响应不暴露userId和数据库辅助列。
4. 数据库约束：事务内故意写非法默认值、空白文本、无父用户、重复默认和显式生成列，按MySQL错误码确认拒绝；多个非默认地址允许存在。测试写入均回滚。
5. 真正并发HTTP：三组同时创建首地址，返回依次为201/201、409/201、201/409；每组保留恰一个默认。两个请求切默认、删除默认与新增竞争也保持默认有效。
6. 真正锁等待：另一事务持有本人地址行锁，正式JAR写入发生MySQL锁等待超时，返回409 ADDRESS_CONFLICTED；失败写入没有改变地址。
7. RR旧快照：先建立旧快照，另一事务修改并提交，再由正式Service切换默认；FOR UPDATE与写后FOR SHARE正确取得新详情。
8. 真实事务回滚：在Mapper已经执行INSERT、清旧默认UPDATE、设新默认UPDATE、DELETE后分别注入异常。实际Service事务代理回滚，事务外读到的已提交地址状态不变。没有用外层测试事务自动回滚冒充业务回滚。
9. 原健康、商品、登录身份、购物车、会话列表HTTP回归通过；无模型调用。

## 环境与数据边界

- 本地Redis没有监听。本次仅对临时测试进程排除Redis Session自动配置，使用Tomcat原生会话完成真实登录/Cookie/CSRF测试；正式application.yml不变。这轮没有重验Redis共享会话、Nginx、双Java或Linux部署。
- 未执行DDL、GRANT、Linux命令、服务迁移、Git提交或推送；没有读取/打印私密配置、模型Key、密码或Cookie。应用自行加载已有项目配置。
- 第一次预检因辅助测试未兼容CHECK异常的JDBC分类而中止；修正的是tmp测试工具，正式业务没有修改。预检17项已运行检查通过，但未完成整组，记录保留：[address-preflight-2026-10-23.json](verification/address-preflight-2026-10-23.json)。后续完整111项为单独运行，不把两轮相加宣称覆盖数。
- 两轮共注册4个随机密码的专用测试账号（id16—19），因应用账号没有app_users DELETE权限，账号保留；不扩大权限清理。所有测试地址已清理为0。原有用户行、商品、购物车、购物车操作、会话、消息与地址的基线内容比较不变；自增计数不属于行内容比较。
- 两个由本轮创建的临时Java进程均已停止，原MySQL服务保留。没有启动Python、VMware、eNSP或Docker Desktop。

## 本组代码要点

请求路径：浏览器 → Security检查身份和CSRF → AddressController → AddressService → UserAddressMapper → MySQL → AddressResponse → JSON。

Authentication是Spring提供的登录身份。CurrentUserService用登录名查询有效用户编号；SQL始终带user_id，不采纳请求里自称的用户编号。

设置默认先锁定本人地址，再在同一事务清旧默认、设新默认。唯一索引负责拦住两个默认；事务保证中途失败不会留下“旧的取消了，新的没设上”。列表为空时并发首次新增仍可能竞争，409提示调用方刷新重试。

下一组：[订单库存准备手写课](ORDER_INVENTORY_FOUNDATION_MANUAL_STEP.md)。地址页面在阶段12一起接；不以地址后端通过宣称整个订单支付完成。
