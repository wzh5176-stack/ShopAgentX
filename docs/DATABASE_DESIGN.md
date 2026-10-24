# 商品数据设计 0.2

2026-10-24通知/补偿与页面复用现表，无DDL/新表/授权。通知查out_trade_no绑定当前应用/商家/金额，复用独立锁事务结算；幂等基于支付状态与官方交易号，不依赖易失notify_id缓存。保留首次可信provider_paid_at，允许查单send_pay_date和通知gmt_payment来源不同；身份金额/官方交易号必须一致。原12表数据摘要保持，新测试账号及终态订单留痕、地址购物车清除、pending/预占0。详TRADE_COMPLETION_ACCEPTANCE.md。

2026-10-24官方关闭复用既有字段，无新表/DDL/授权。支付CLOSED表示已按关单流程确认结束：provider_trade_no记录关单成功的官方交易号，provider_paid_at仍NULL；付款链接截止后另5分钟且再查无交易的关闭无官方交易号。官方确认先独立提交，订单关闭与库存释放另一个事务；后者失败可保留关闭确认重试，两种新支付创建都拒绝穿过此间隙。原MOCK事务语义不变。432项验收通过，原业务表摘要未变，见ALIPAY_CLOSE_ACCEPTANCE.md；真实沙箱与Linux迁移未验。

2026-10-24官方查单结算复用027，无新DDL/授权。成功在一个事务消费product_inventory.reserved_quantity、改shop_orders为PAID、改payment_attempts为SUCCEEDED并保存provider_trade_no/provider_paid_at；唯一官方交易号重复时整事务回滚。订单paid_at是本地入账时间，provider_paid_at保留官方send_pay_date经上海→业务Clock转换。新组107＋MOCK回归137通过，原12表行摘要均保持；见ALIPAY_QUERY_ACCEPTANCE.md。官方关单/通知与Linux迁移尚未实现。

2026-10-24实核：Windows MySQL8.0.34/shopagent 的027五列out_trade_no/provider_app_id/provider_seller_id/provider_trade_no/provider_paid_at及唯一/身份/成功证据约束已存在，旧生成列保留。本次没有执行DDL/GRANT。实际拒绝缺身份、缺成功证据、MOCK携带官方元数据，并回滚测试改动；原12表行摘要未变。证据verification/alipay-checkout-formal-2026-10-24.json，说明见ALIPAY_CHECKOUT_ACCEPTANCE.md。Linux未迁移，下面旧“027未执行”为历史。

2026-10-24最新支付实库：023 payment_attempts原已存在，已实核MySQL8.0.34本地shopagent；新增025 mock_payment_receipts（payment_id主键引用支付、order_id引用订单、精确amount、SUCCEEDED/FAILED不可变outcome与时间），执行024支付表SELECT/INSERT/UPDATE、026回执仅SELECT/INSERT应用授权，无其他表权限扩张/删除。证据verification/payment-schema-2026-10-24.json及MOCK_PAYMENT_ACCEPTANCE.md，支付已正式验收；下面“支付表候选/未执行”属历史。回执独立提交、商户支付/订单/库存另一个原子事务；原12表行在支付两轮均不变。尚未迁移Linux，不能将新JAR直接部署到未迁移新表的环境。

2026-10-24支付表候选：payment_attempts关联shop_orders，订单+请求键唯一，PENDING/SUCCEEDED/FAILED/CLOSED，DECIMAL(14,2)金额与毫秒时间；生成active_order_id/successful_order_id分别唯一限每单一待处理/一成功，不映射实体。跨表金额/到期/归属与跨状态互斥仍靠锁订单的Service。023/024只在MOCK_PAYMENT_FOUNDATION_MANUAL_STEPS.md给用户手写，未代建正式SQL/执行/核实新表，不声明支付表已存在。

2026-10-24最新：阶段10本机后端已完成，本轮没有新迁移/DDL/GRANT。取消/自动过期通过同事务订单FOR UPDATE、明细FOR SHARE与库存条件UPDATE完成，终态不会再释放；商品新增和零库存行同事务。当前构建108项生命周期＋96项建单回归通过，原10表各轮既有行哈希不变。测试合成账号/隐藏商品/终态订单留痕，各轮测试待支付/预占0，详情见[生命周期验收](ORDER_LIFECYCLE_ACCEPTANCE.md)。下文旧“取消尚未接”“订单0行”均为历史时点；支付表尚未创建。

2026-10-24建单验收更新：原有10表业务行内容比较不变；额外保留7个合成测试订单及明细，全部由测试辅助SQL关闭并释放预占，另有2个专用测试账号、3个已逻辑删除/下架的测试商品和2行测试库存。不是正式取消功能，实际未接取消接口；测试地址已清理、预占与待支付测试单均0。下方2026-10-23的“订单/明细0行”是验收前结构核对时点，不代表本次测试后表仍为空。详情见[建单验收](ORDER_CREATE_ACCEPTANCE.md)。自增号因插入和回滚出现空缺是正常现象，不承诺连续。

## 阶段10订单与库存表（2026-10-23结构权限已核）

完整SQL/实体/Mapper见[订单合并手写教案](ORDER_TABLES_AND_ENTITIES_MANUAL_STEPS.md)。用户授权检查并直接补齐缺失部分后，助手已从上组教案恢复021/022 SQL文件。真实Windows本地MySQL8.0.34/shopagent以shopagent_app@127.0.0.1连接，三表及权限原已存在，因此没有执行DDL/GRANT。库存6行，主单/明细各0行，19项列/索引/FK/CHECK/引擎/精确权限核对通过，详见[SQL恢复记录](ORDER_SQL_RECOVERY.md)与verification/order-sql-2026-10-23.json。不是建单/超卖/回滚业务验收。

shop_orders用signed BIGINT自增id，user_id引用既有app_users。每用户request_key唯一＋规范化request_hash支持后续幂等比较；六项地址快照不引用可删除的地址表；金额DECIMAL(14,2)>0。四状态及对应时间CHECK、15分钟过期规则后续由Service执行。索引user_id/id供本人列表、status/expires_at/id供过期扫描；CHECK不自动执行状态转换。

order_items引用订单和商品，UNIQUE(order_id,product_id)保证一单一商品一行；保存商品标题/品牌/图片/单价快照，数量1—99，CHECK行小计=单价×数量。至少一明细、最多20种及总金额等于各行之和由Service同事务保证。订单和明细不级联删除或物理删除；既有商品采用逻辑删除。应用账号022候选只授主表SELECT/INSERT/UPDATE、明细SELECT/INSERT，库存020保留SELECT/INSERT/UPDATE；不扩旧表或用户表权限。

本机已建表无需再运行021/022。未来迁移其他环境仍先核目标和实际已有结构；DDL隐式提交，部分失败不能自动DROP或盲重跑。后续Service/HTTP接齐后集中验证快照和幂等、并发超卖/失败回滚、取消/过期释放及旧业务回归；此处不是已实现完整订单。

## 第二版阶段9：用户地址表（2026-10-23已核）

用户手写017_create_user_addresses.sql已在Windows本地shopagent执行；本轮以应用账号独立确认实际MySQL8.0.34、地址表/列/索引/启用约束，并以回滚写入验证外键、CHECK和唯一生成列约束，见ADDRESS_ACCEPTANCE.md。没有代执行DDL或授权，不用于未核实的MariaDB。

user_addresses保存id、user_id、收货人、电话、省市区、详细地址、is_default与创建/更新时间；外键引用app_users。物理删除地址；阶段10订单必须保存独立地址快照，不通过级联删除破坏历史订单。

default_user_id是STORED生成列，默认地址取user_id，其他取NULL；唯一索引利用多个NULL可并存实现每用户最多一个默认地址。CHECK限制默认标记0/1与各项非空白，普通用户索引支持本人查询。Java实体不声明/写入该生成列。第一条默认和删除后的接替由事务Service实现；首次创建及切默认/删除竞争、锁超时409、四种SQL后故障回滚和RR快照当前读均已实测，不能声称一个索引就解决全部业务。

018权限已确认：shopagent_app@127.0.0.1对user_addresses恰为SELECT/INSERT/UPDATE/DELETE；未增加app_users UPDATE/DELETE。两轮保留四个专用测试账号id16—19，测试地址已清理，原业务行内容不变；没有重置旧表或清空数据。

阶段10 product_inventory已真实确认6行、列/引擎/启用约束及应用账号SELECT/INSERT/UPDATE权限，product_id主键同时引用products.id；available_quantity可售、reserved_quantity待支付占用。初始化教案为在售演示20/0、其余0/0，本次只查结构与总行数，未独立核每件商品当前数量或执行补货。正式文件已存在；库存业务/订单仍待接线和验收，新商品创建初始化0与取消/支付原子更新不能遗漏。

更新：2026-10-12。第一版七字段与五条虚构种子已建立，公开查询已正式验收；阶段2b新增逻辑删除列deleted，原商品仍4上架/1下架且全部未删除。管理写接口与Java逻辑删除映射待实现/集中验收。

## 当前业务目标

先做商品列表、详情与标题/品牌关键词搜索。示例流程：用户浏览列表，搜索“跑鞋”，点击某个商品查看介绍。后续 AI 使用真实商品信息解释推荐；当前数据全部是学习样例。

数据库为 shopagent，表为 products。2026-10-11 用户同意创建后，先复查本机 MySQL 8.0.34 上无同名库，再执行 backend/sql/001_create_shopagent.sql，退出码 0。表名与自己的接口 /api/products 对应，本项目单独建库。

## 商品表字段：已实现的第一版

一行代表一件商品；列代表这件商品的属性。

| 数据库字段 | 实际类型 | 用途与规则 | 对应接口字段 |
| --- | --- | --- | --- |
| id | BIGINT，自增主键 | 唯一识别商品；正整数，当前种子 ID 在 JavaScript 安全整数范围内 | id |
| title | VARCHAR(200) NOT NULL | 商品名称，不能为空；列表与搜索使用 | title |
| brand | VARCHAR(100) NOT NULL | 品牌，不能为空；列表与搜索使用 | brand |
| price | DECIMAL(10,2) NOT NULL | 人民币元，非负；数据库与 Java 中进行精确数值处理 | price，JSON 两位小数字符串 |
| image_url | VARCHAR(500)，允许 NULL | 商品主图地址；没图片时返回 null | imageUrl |
| description | TEXT NOT NULL | 商品介绍；可为空字符串；详情中展示 | description |
| status | TINYINT NOT NULL，默认 1 | 1=上架，0=下架；列表和详情只提供上架商品 | 不公开，作为服务端过滤条件 |
| deleted | TINYINT NOT NULL，默认 0 | 2026-10-12迁移增加；0=未删除，1=已删除；CHECK限制0/1 | 不由请求DTO接收，不公开 |

后续数据库 price 对应 Java BigDecimal，接口按 API_CONTRACT.md 输出 "299.00" 等两位小数字符串，Java 商品代码尚未实现。DECIMAL(10,2) 的 10 是总位数、2 是小数位数。实际库表为 utf8mb4/utf8mb4_unicode_ci，表使用 InnoDB；已落实并查到 ENFORCED=YES 的四项 CHECK：名称和品牌去除两端空格后非空、价格非负、状态仅 0/1。没有执行非法插入测试，不将结构核对称为完整业务验证。

## 实际验证证据

- SHOW CREATE DATABASE shopagent：数据库存在，字符集与排序规则符合脚本。
- SHOW CREATE TABLE shopagent.products：七个字段、id 自增主键、默认上架、允许空图片及四项 CHECK 均符合设计。
- information_schema.TABLE_CONSTRAINTS：四项 CHECK 的 ENFORCED 均为 YES。
- 首次建表时 COUNT 为 0；随后空表复查通过后执行 002_seed_products.sql，插入 5 条虚构样例。003_verify_products.sql 独立查询结果为总数 5、上架 4、下架 1；全部字段、中文、价格小数、NULL 图片与状态符合脚本。
- status=1 返回 4 条；标题/品牌包含“跑鞋”且上架返回 2 条（轻量跑鞋A、缓震跑鞋B），下架的旧款跑鞋E 未返回。仅为 SQL 验证，商品 API 和 Java 数据库连接尚未实现。
- 初始化脚本只用于首次创建，不含 DROP、凭据或种子；不把对同名库的重复执行报错当作已完成迁移。

只为当前查询场景建立必要索引；初期 5～10 条样例仅需主键，不宣称包含关键词 LIKE 查询可以被普通索引加速。后续根据实际查询与 EXPLAIN 决定。

## 学习样例与验收准备

目前使用 5 条样例：轻量跑鞋A（299.00，上架）、缓震跑鞋B（459.00，上架）、训练鞋C（699.00，上架）、无线耳机D（199.00，上架）、旧款跑鞋E（99.00，下架）。前四条 id 为 1～4，下架条目 id 为 5。全部图片为 NULL，后续页面使用无图展示；尚未验证非空图片地址。所有名称、价格为虚构练习数据，不代表真实商品或售价。

002_seed_products.sql 为一次性初始插入，重复执行会新增相同内容；003_verify_products.sql 为只读核对，可重复查询。没有替用户删除/重置表。

后续验证：列表只返回上架商品；标题/品牌搜索与样例相符；已知 ID 的详情字段一致；下架/不存在 ID 返回 404；价格保留两位小数，图片缺失返回 null。分页与错误细节以 API_CONTRACT.md 为准。

## 参考与取舍

### 2026-10-12逻辑删除迁移

助手执行backend/sql/005_add_product_logical_delete.sql，仅修改自己项目的products表结构。五条商品全部deleted=0，原七字段行数与CRC32聚合校验和前后相同，没有删除或修改原商品属性；证据docs/verification/product-logical-delete-schema-2026-10-12.txt。未授写权限、未读取/重置应用密码；项目账号仍只读。

status=0表示下架，可管理编辑和再上架；deleted=1表示业务删除，普通管理与公开查询均排除。用户补@TableLogic(value="0", delval="1")后，MyBatis-Plus生成的BaseMapper查询/删除支持该规则；手写SQL不自动过滤。当前实体/HTTP效果尚未验收，不把表迁移当成删除接口完成。完整教学见PRODUCT_LOGICAL_DELETE_STEP.md。

已读参考 Product.java、ProductMapper.java 及 init.sql 的 product/product_sku 建表段落。参考采用 MyBatis-Plus，包含 category_id、base_price、SKU、评价、销量等。自己的当前表使用 price，先提供列表/详情，分类、规格、库存等在对应需求出现时再扩展；未来 Agent 购物车接入时需重新设计商品与具体规格的关系。

参考 SQL 含 DROP TABLE；本项目不直接执行其 init.sql，自己的建表脚本在确认专用库后单独准备。

[MySQL 8.0 官方：精确小数类型](https://dev.mysql.com/doc/refman/8.0/en/fixed-point-types.html)
