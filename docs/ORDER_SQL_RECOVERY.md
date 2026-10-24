# 021/022脚本恢复与数据库只读核对

日期：2026-10-23。用户明确授权：“我忘记了，你检查吧，没有的话你直接帮我搞了，继续吧”。本次授权针对此前缺失的订单SQL文件及相应建表/授权准备，不扩为自动代写后续Java业务。

## 已完成

- 新建D:\ShopAgent-X\backend\sql\021_create_orders.sql，内容从上组完整教案的建表SQL原样恢复。
- 新建D:\ShopAgent-X\backend\sql\022_grant_orders.sql，内容从上组完整教案的最小授权SQL原样恢复。
- 使用已有编译应用的数据源配置，只读连接Windows本机MySQL8.0.34/shopagent，实际身份shopagent_app@127.0.0.1。未手动读取/打印私密配置值。
- product_inventory已存在且6行；shop_orders、order_items都已存在且各0行。三表列集合、InnoDB、启用约束、关键FK/唯一键与精确表级权限共19项元数据检查通过。
- shop_orders确有每用户request_key唯一、用户FK、6个CHECK且不引用地址表；order_items确有订单/商品双FK、每单商品唯一和4个CHECK。应用账号库存/主订单SELECT INSERT UPDATE，明细SELECT INSERT，与教案一致。

证据：[order-sql-2026-10-23.json](verification/order-sql-2026-10-23.json)。本次是结构/行数/权限核对，不是实际建单、并发、回滚或端到端业务验收。

数据库已有正确结构和权限，因此没有执行CREATE/ALTER/GRANT、没有重跑迁移或修改旧数据。用户无需再执行021/022。辅助探针位于忽略目录tmp/address-formal-audit，启动的是先前编译应用的临时本地上下文，使用随机端口，临时排除Redis Session自动配置，完成后关闭。未改变正式配置、未启动模型/部署/VM/eNSP、未Git提交推送。

下一继续[事务创建订单手写组](ORDER_TRANSACTION_CREATE_MANUAL_STEPS.md)，Java仍由用户手写，接口与其他相关功能接齐后集中验收。
