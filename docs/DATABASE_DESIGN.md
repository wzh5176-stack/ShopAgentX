# 商品数据设计 0.2

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
