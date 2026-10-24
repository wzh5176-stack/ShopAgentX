# 阶段11第一组：支付单表与支付状态（手写）

日期2026-10-24。阶段10订单库存后端已验收；用户要求从模拟支付恢复手写。本组只是教案，助手没有代建正式SQL/Java文件、执行SQL、编译或验收支付功能。现有订单源已只读核对；本地MySQL上轮为8.0.34，本次执行前仍先运行下方目标查询。

## 本阶段路线

1. 本组：支付单表、应用账号权限、支付状态枚举。
2. Java实体/Mapper、请求响应DTO、本人支付尝试创建与查询。创建锁订单并核归属/到期/金额；金额读取订单，不由前端决定。相同订单同请求键回放，不同键若已有PENDING则复用当前尝试，不创建第二笔；已成功则不能新建。旧键指向旧失败记录时仍回放，用户新尝试才换键。
3. 本地模拟渠道接口与服务端结果：成功时，同一事务订单PAID、支付单SUCCEEDED、预占减少；可售数不再减少。模拟失败只把支付单记FAILED，订单仍可重试。结果从后端渠道边界查询核实，不将网页传来的金额/status直接当成功凭证；演示触发仅在本地模拟模式且本人身份/CSRF保护下提供。
4. 重复结果、失败后重试、支付与取消/到期竞争、查询补偿、集中真实事务与HTTP验收。统一先锁订单再锁支付记录，再按商品id排序操作库存，避免反向锁顺序。取消/到期后支付单可能短暂仍PENDING，后续关闭/查询补偿同步；成功处理必须复核订单状态及截止时间，不能因支付表仍PENDING就越过已关闭订单。

本地模拟不需要支付账号、密钥或真实资金。官方沙箱与页面/Linux迁移仍为阶段12。

## 为什么独立支付单

shop_orders保存买什么、应付金额、地址和购买快照；payment_attempts保存某一次付款尝试。一个订单可以先有FAILED的尝试，再用新请求键重试并SUCCEEDED。订单不会因一次支付失败就变为自造的PAYMENT_FAILED。

例如订单100应付598.00：尝试1失败后，订单仍PENDING_PAYMENT；尝试2成功后，订单PAID。以下编号和金额只是例子，不是实际运行输出。

支付状态：PENDING处理中、SUCCEEDED成功、FAILED本次失败、CLOSED本次关闭。支付单CLOSED和订单CANCELLED/EXPIRED含义不同。

## 第一步：新建023_create_payment_attempts.sql

完整路径：D:\ShopAgent-X\backend\sql\023_create_payment_attempts.sql。直接放backend/sql，别在里面重复新建sql目录。

```sql
USE shopagent;

SELECT DATABASE(), VERSION(), CURRENT_USER(), @@hostname, @@port;
SHOW TABLES LIKE 'payment_attempts';

CREATE TABLE payment_attempts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    request_key VARCHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    channel VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin
        NOT NULL DEFAULT 'MOCK',
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin
        NOT NULL DEFAULT 'PENDING',
    amount DECIMAL(14, 2) NOT NULL,
    expires_at DATETIME(3) NOT NULL,
    finished_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),

    active_order_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN status = 'PENDING' THEN order_id ELSE NULL END
    ) STORED,
    successful_order_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN status = 'SUCCEEDED' THEN order_id ELSE NULL END
    ) STORED,

    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_attempts_order_request (order_id, request_key),
    UNIQUE KEY uk_payment_attempts_active (active_order_id),
    UNIQUE KEY uk_payment_attempts_success (successful_order_id),
    KEY idx_payment_attempts_order_id (order_id, id),
    KEY idx_payment_attempts_expiry (status, expires_at, id),

    CONSTRAINT fk_payment_attempts_order
        FOREIGN KEY (order_id) REFERENCES shop_orders (id),
    CONSTRAINT chk_payment_attempts_request
        CHECK (CHAR_LENGTH(request_key) = 36),
    CONSTRAINT chk_payment_attempts_channel
        CHECK (channel = 'MOCK'),
    CONSTRAINT chk_payment_attempts_status
        CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED', 'CLOSED')),
    CONSTRAINT chk_payment_attempts_amount
        CHECK (amount > 0),
    CONSTRAINT chk_payment_attempts_expiry
        CHECK (expires_at > created_at),
    CONSTRAINT chk_payment_attempts_state_time
        CHECK (
            (status = 'PENDING' AND finished_at IS NULL)
            OR
            (status IN ('SUCCEEDED', 'FAILED', 'CLOSED')
                AND finished_at IS NOT NULL
                AND finished_at >= created_at)
        )
) ENGINE=InnoDB
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

SHOW CREATE TABLE payment_attempts;
```

先在DataGrip的Windows本机localhost:3306/shopagent、root连接控制台，只执行USE/SELECT/SHOW三段，确认目标和版本、表尚不存在，再选CREATE到末尾执行。已有表则查看结构，不重跑CREATE、不DROP。不要选Linux部署库，不重跑旧001—022。这里给的不是已执行SQL。

### 重要字段

- id：支付尝试自己的编号；order_id：对应订单编号，一个订单可有多次尝试。
- request_key：同一次创建支付请求重试沿用UUID，唯一索引避免同订单同键重复插入。后续Java负责UUID格式、归属和内容语义，索引不是全部业务。
- channel：当前固定MOCK。阶段12接官方沙箱时再迁移渠道约束与渠道字段；此处并未支持沙箱。
- amount：精确金额，必须从shop_orders.total_amount复制；DECIMAL(14,2)与订单一致，不能用float/double。
- expires_at：由后端设置，不晚于订单截止时间；数据库CHECK不能跨表检查，后续Service负责。
- finished_at：PENDING为空，成功/失败/关闭时保存结束时间。DATETIME(3)保留毫秒，沿用现有订单Clock/LocalDateTime约定，不新增时区策略。

### 两个生成列

active_order_id在PENDING时等于order_id，其他状态为NULL。唯一索引意味着同一订单最多一条PENDING；历史失败记录可有多条，因为MySQL唯一索引允许多个NULL。successful_order_id同理限制每单最多一条SUCCEEDED。和地址表的默认地址生成列思路相同。

两个列由数据库计算，未来Java实体不声明、不设置它们。这两个索引各管一种状态，**不能跨列阻止一笔成功与另一笔待支付并存**；后续Service还必须锁同一订单、检查成功/待支付并复用，状态转换也同样锁订单，不能只靠索引保证整个支付规则。

CHECK只拒绝非法单行数据，不会主动查询渠道、自动计时或更新订单/库存。

## 第二步：新建024_grant_payment_attempts.sql

完整路径：D:\ShopAgent-X\backend\sql\024_grant_payment_attempts.sql。

```sql
USE shopagent;

GRANT SELECT, INSERT, UPDATE
    ON shopagent.payment_attempts
    TO 'shopagent_app'@'127.0.0.1';

SHOW GRANTS FOR 'shopagent_app'@'127.0.0.1';
```

同一个Windows本地root连接执行，且023先成功。Java用的shopagent_app需要读、创建和更新本次支付单，没有DELETE；保存失败记录用于解释和重试。这里不修改Linux的shopagent_app@%，不新增账号/密码，不扩大其他表权限。

## 第三步：新建PaymentStatus.java

完整路径：D:\ShopAgent-X\backend\src\main\java\com\wwzyhy\shopagent\dto\PaymentStatus.java。

```java
package com.wwzyhy.shopagent.dto;

public enum PaymentStatus {
    PENDING,
    SUCCEEDED,
    FAILED,
    CLOSED
}
```

enum是有限的固定选项，与现有OrderStatus用法相同。以后PaymentStatus.SUCCEEDED.name()得到字符串SUCCEEDED，供数据库status使用；暂时不需要创建对象、写构造器或启动它。

## 本组结束位置

只完成这三个文件与本地SQL操作；尚无支付HTTP接口，也不会自动改变旧订单/库存。下一组PaymentAttempt实体、PaymentAttemptMapper与支付请求/响应DTO，再接Service；几个相关步骤后集中核对。不必启动Python、VMware或进行全项目验收。
