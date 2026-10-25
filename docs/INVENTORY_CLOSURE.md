# 库存闭环与合并发布

2026-10-25完成管理员补货、库存展示与导购缺货过滤，并将自动化回归、36款商品与RAG更新一起升级至现有VMware环境。本轮没有新增订单或调用付款接口。

## 行为

- 商品创建后库存仍为0；管理员通过补货接口增加可售数量，不直接覆盖可售或预占值。
- 数量范围1–1000000，必须说明原因。操作记录保存管理员、请求编号、商品、数量、原因及当次库存快照。
- 同一管理员重复提交同一请求和内容，返回原记录；复用编号但改变商品、数量或原因返回409。
- 库存增加和记录插入处于同一事务，记录失败时库存回滚；原子增加含INT溢出保护，不修改订单预占。
- 公共商品列表和详情返回`availableQuantity`，页面显示可售数量/缺货，缺货详情禁用加入购物车。
- 导购查询在数据库候选截断前过滤可售库存大于0的商品。页面库存只是快照，实际下单仍由原事务重新核对和预占。

## 管理入口与接口

`/shopagent/inventory.html`复用原管理员登录。首页页脚有入口；普通用户不能读取或操作管理员接口。页面显示当前库存、补货表单和最近20条补货记录。

| 接口 | 作用 |
| --- | --- |
| `GET /api/admin/products/{id}/inventory` | 当前可售与预占数量 |
| `GET /api/admin/products/{id}/inventory/replenishments` | 最近20条补货记录 |
| `POST /api/admin/products/{id}/inventory/replenishments` | 补货，要求ADMIN与CSRF |

```json
{"requestKey":"example_unique_request_001","quantity":10,"reason":"演示仓库入库"}
```

响应`data`包含记录`id/productId/requestKey/quantity/reason/actor/availableQuantity/reservedQuantity/createdAt`。操作者来自服务端认证身份，不接受前端指定。库存快照是当次操作结果，重试原请求不承诺返回之后其他操作产生的实时余额；实时余额请GET库存。

页面在请求结果不明时保留同一编号，冻结修改项并允许重试核对；同一标签页刷新后可恢复未确认请求。浏览器存储不可用时只保留内存状态，重新打开页面应先查看记录。不会把超时当成补货失败后自动新建另一笔操作。

旧导购历史仍可读取：历史商品快照缺少库存时保持null，不伪装成实时库存0；浏览详情时重新查当前库存。商品数量展示使用批量库存查询，避免列表逐商品查库。

## 结构升级与权限

新增`028_create_inventory_replenishments.sql`和`029_grant_inventory_replenishments.sql`。001–027内容保持不变；经审查追加清单，空库基线更新为13表、17条结构语句。没有向已有库重放基线。

新表`inventory_replenishments`用`(actor,request_key)`唯一键、商品外键和数量约束。应用账号仅有新表SELECT/INSERT权限，不提供修改审计记录的接口。Windows开发账号host为127.0.0.1；部署账号沿用现有%，在升级工具中单独授权。

## 验收

- 集中回归121项通过：Java63、真实MySQL业务12、Python36、工程8、导入MySQL2。
- 新增真实数据库检查涵盖16次同键并发只加一次、16笔不同请求数量不丢、预占保持、请求冲突、整数溢出、审计插入失败回滚、21件缺货候选不挤掉可售商品。
- 实际Security过滤链检查管理员角色、CSRF、匿名与普通用户权限。
- Edge在390px和1440px检查登录/拒绝、库存展示、结果丢失后的同键重试与刷新恢复、缺货按钮；这组页面API是受控替身。
- Linux实际Java/MySQL/Redis/Python/Nginx联调通过：创建明确标注的验收商品→0库存→补货7→同键重试仍7→变更内容409→一条审计记录→公共详情显示7。验收商品最终逻辑删除，审计记录保留；未改已有商品库存。
- 已用新目录做真实DeepSeek资料回答：E02无线充电得到对应版本引用；未记载的保修问题返回无依据。检索72正例/42负例的详细范围仍见[商品资料与RAG](CATALOG_RAG.md)。

本轮没有补验真实沙箱WAIT_BUYER_PAY主动关单，也没有重新付款；已有支付保护和配置保留。

## 部署与回退

本机升级前完整备份在`D:\ShopAgent-backups\inventory-before-20261025-1210.sql`。Linux完整备份在`/home/shopagent/shopagent/backups/before-inventory-20261025-120826.sql`，SHA256为`55abfb66d76ae3b3b1d5f7ca0fd81e2fbe3e6ad0841516a20759f96edfdd4063`。备份、真实配置与验收原始材料均不上传GitHub。

Linux发布目录为`/home/shopagent/shopagent/releases/inventory-20261025`。原12张表的旧数据指纹在升级与新增目录导入后保持一致；新增36款后仍为40件在售。没有Windows全库覆盖Linux、清空数据卷或重置旧库存。

当前镜像为`shopagent-backend:inventory-20261025`、`shopagent-ai:inventory-20261025`，双Java与MySQL/Redis/Python/Nginx共六容器健康。AI增量镜像复用原环境，并检查所有固定依赖版本及pip一致性；全新构建仍用标准`ai-service/Dockerfile`。额外的operator镜像仅用于目录导入，业务AI容器不安装MySQL驱动。

`runtime.sh`及`trade-runtime.sh`通过本机激活标记追加`compose.inventory.yml`。标记不进入Git。旧镜像保留为`*:pre-inventory-20261025`，旧runtime脚本另存发布目录。若需回退，先安排维护窗口，停用库存覆盖文件并启动原镜像；新增表和真实补货数据保留，不为回退删除账本、改回库存或删除卷。

真实支付通知隧道没有在本轮重开。远端CI结果应按GitHub当前提交的Actions记录核对，不能用本地测试冒充远端通过。
