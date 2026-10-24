# 阶段10第二组：订单主表、明细、实体与Mapper

日期：2026-10-23。用户要求一次多教几步，本组给完整两表SQL、状态枚举、两个实体、两个Mapper和本地授权，共七个正式文件，由用户手写。上一组019/020与ProductInventory/Mapper正式文件已只读看到；本轮不编译正式应用、不核数据库、不执行SQL/授权/请求，不把文件存在当库存已经验收。

## 本组目标与整体请求路径

这一阶段的请求路径将是：用户选商品和地址 → Java验证登录用户、本人地址、真实商品与现价 → 同一数据库事务写订单/明细并预占库存 → 返回待支付订单 → 本人列表/详情 → 取消或超时释放库存。阶段11才接本地模拟支付，阶段12再官方沙箱和页面。

现在先把持久化结构写好，尚无订单HTTP路由。数据库本机MySQL8.0.34已经在上一轮地址验收实核，下面按MySQL8.0.16+的启用CHECK设计；不当成已验证的MariaDB迁移。父表主键沿用现有signed BIGINT。

一张订单主表shop_orders保存一个订单的用户、总额、状态、六项地址快照与时间；多行order_items保存该订单各个商品的购买快照。订单直接保存地址文本，不外键引用可删除的user_addresses。明细引用产品编号以保留关联，同时自己保存标题/品牌/图片/价格；读历史不能重新套用当前products的价格和标题。

## 1. 新建021建表SQL

文件：D:\ShopAgent-X\backend\sql\021_create_orders.sql

在DataGrip现有Windows本地127.0.0.1:3306管理连接操作。先只执行USE/SELECT/两个SHOW TABLES，核目标shopagent且两表都不存在，再执行CREATE段。不要重跑001—020，不操作Linux库。若其中一个CREATE报错，先保留错误并核已建哪张表；MySQL DDL不是普通事务可完整回滚的操作，不删表盲重跑。

```sql
USE shopagent;

SELECT DATABASE(), VERSION(), CURRENT_USER(), @@hostname, @@port;
SHOW TABLES LIKE 'shop_orders';
SHOW TABLES LIKE 'order_items';

CREATE TABLE shop_orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    request_key VARCHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    request_hash VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin
        NOT NULL DEFAULT 'PENDING_PAYMENT',
    total_amount DECIMAL(14, 2) NOT NULL,

    recipient_name VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    province VARCHAR(50) NOT NULL,
    city VARCHAR(50) NOT NULL,
    district VARCHAR(50) NOT NULL,
    detail_address VARCHAR(200) NOT NULL,

    expires_at DATETIME(3) NOT NULL,
    paid_at DATETIME(3) NULL,
    closed_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uk_shop_orders_user_request (user_id, request_key),
    KEY idx_shop_orders_user_id (user_id, id),
    KEY idx_shop_orders_expiry (status, expires_at, id),

    CONSTRAINT fk_shop_orders_user
        FOREIGN KEY (user_id) REFERENCES app_users (id),
    CONSTRAINT chk_shop_orders_request
        CHECK (CHAR_LENGTH(request_key) = 36
            AND CHAR_LENGTH(request_hash) = 64),
    CONSTRAINT chk_shop_orders_amount
        CHECK (total_amount > 0),
    CONSTRAINT chk_shop_orders_status
        CHECK (status IN (
            'PENDING_PAYMENT', 'PAID', 'CANCELLED', 'EXPIRED'
        )),
    CONSTRAINT chk_shop_orders_address
        CHECK (
            CHAR_LENGTH(TRIM(recipient_name)) > 0
            AND CHAR_LENGTH(TRIM(phone)) > 0
            AND CHAR_LENGTH(TRIM(province)) > 0
            AND CHAR_LENGTH(TRIM(city)) > 0
            AND CHAR_LENGTH(TRIM(district)) > 0
            AND CHAR_LENGTH(TRIM(detail_address)) > 0
        ),
    CONSTRAINT chk_shop_orders_expiry
        CHECK (expires_at > created_at),
    CONSTRAINT chk_shop_orders_state_time
        CHECK (
            (status = 'PENDING_PAYMENT'
                AND paid_at IS NULL AND closed_at IS NULL)
            OR
            (status = 'PAID'
                AND paid_at IS NOT NULL AND paid_at >= created_at
                AND closed_at IS NULL)
            OR
            (status IN ('CANCELLED', 'EXPIRED')
                AND paid_at IS NULL
                AND closed_at IS NOT NULL AND closed_at >= created_at)
        )
) ENGINE=InnoDB
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE TABLE order_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    brand VARCHAR(100) NOT NULL,
    image_url VARCHAR(500) NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    quantity INT NOT NULL,
    line_amount DECIMAL(14, 2) NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_order_items_order_product (order_id, product_id),
    KEY idx_order_items_product (product_id),

    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES shop_orders (id),
    CONSTRAINT fk_order_items_product
        FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_order_items_text
        CHECK (CHAR_LENGTH(TRIM(title)) > 0
            AND CHAR_LENGTH(TRIM(brand)) > 0),
    CONSTRAINT chk_order_items_price
        CHECK (unit_price >= 0),
    CONSTRAINT chk_order_items_quantity
        CHECK (quantity BETWEEN 1 AND 99),
    CONSTRAINT chk_order_items_amount
        CHECK (line_amount = unit_price * quantity)
) ENGINE=InnoDB
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

SHOW CREATE TABLE shop_orders;
SHOW CREATE TABLE order_items;
```

### 表和字段的含义

同一个订单买耳机2件、鼠标1件：shop_orders一行，order_items两行。order_items.order_id都指向同一个shop_orders.id。一行明细代表一种商品及数量，不是每一件单独一行。

unit_price是下单时数据库中的单价；quantity是购买数量；line_amount是该行小计。示例耳机299.00×2=598.00，鼠标99.00×1=99.00，订单total_amount=697.00。金额在Java用BigDecimal，数据库用DECIMAL，后续响应仍输出两位小数字符串。

明细CHECK验证单行小计；订单总额等于全部明细之和、订单至少一行、最多20种商品，需要Service在同一事务中保证，普通CHECK不会跨表求和。每种数量1—99，重复productId拒绝，一种商品一行。DECIMAL(14,2)足以容纳20种现有DECIMAL(10,2)最大单价×99数量的总额。本轮演示只创建应付金额>0的订单，所有商品合计0元时拒绝；0元专用流程另议。

request_key是客户端为一次下单生成的小写标准UUID；同一次重试沿用，明确发起新订单才换。它不是登录Token或支付凭证。request_hash是Java对规范化后的addressId及按productId排序的商品编号/数量做SHA-256得到的64位十六进制摘要。用户归属来自Authentication，不来自此摘要。

UNIQUE(user_id,request_key)使同一用户同一请求键最多一单；相同键相同内容回放旧单，相同键不同内容409。普通请求唯一键不是完整的幂等实现：后续必须由Service比较hash；并发唯一冲突要在失败事务外回查，库存预占随失败事务回滚。合法重试先找旧单，不重新按当前价格/库存/地址存在性拒绝旧单；hash不放当前价格或地址文本，避免原请求未变却因后台改价/地址变化被误判。不同键不自动合并相似订单，客户端必须复用同一逻辑下单的键。

订单内六项地址来自本人地址记录复制，是下单快照；不接收客户端自报收货文本，不要求旧地址永远存在。订单明细标题/品牌/图片/单价来自真实商品复制；逻辑删除或下架不抹掉历史快照。主表用户外键、明细订单/商品外键均不配置级联删除；本轮订单不物理删除。

expires_at计划建单后15分钟；paid_at仅成功付款设置，closed_at仅取消/过期设置。状态与时间CHECK防止基本组合错误，但不会自动判断是否真的超时或收到支付成功；这些检查必须由后续Service完成。DATETIME(3)保留毫秒，不包含时区；沿用现有Clock配置，建单显式设置createdAt与expiresAt并统一毫秒精度，Linux迁移时核对Java/数据库时区。

三个主表索引各有用途：user_id+request_key唯一兜底幂等；user_id+id查本人订单；status+expires_at+id供后续查过期待支付订单。索引本身不会自动取消订单。订单状态不设置PAYMENT_FAILED：支付尝试失败时订单可仍待支付，支付尝试状态属于阶段11独立支付单。

## 2. 新建状态枚举

文件：D:\ShopAgent-X\backend\src\main\java\com\wwzyhy\shopagent\dto\OrderStatus.java

```java
package com.wwzyhy.shopagent.dto;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    CANCELLED,
    EXPIRED
}
```

对应待支付、已支付、已取消、已过期。枚举不会自己切状态；后续Service在事务中操作状态与库存。订单实体沿用现有CartAction的String状态字段风格，用OrderStatus.PENDING_PAYMENT.name()取得PENDING_PAYMENT字符串，无需增加枚举类型处理器。

## 3. 新建订单实体

文件：D:\ShopAgent-X\backend\src\main\java\com\wwzyhy\shopagent\entity\ShopOrder.java

```java
package com.wwzyhy.shopagent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("shop_orders")
public class ShopOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("request_key")
    private String requestKey;

    @TableField("request_hash")
    private String requestHash;

    private String status;

    @TableField("total_amount")
    private BigDecimal totalAmount;

    @TableField("recipient_name")
    private String recipientName;

    private String phone;
    private String province;
    private String city;
    private String district;

    @TableField("detail_address")
    private String detailAddress;

    @TableField("expires_at")
    private LocalDateTime expiresAt;

    @TableField("paid_at")
    private LocalDateTime paidAt;

    @TableField("closed_at")
    private LocalDateTime closedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRequestKey() {
        return requestKey;
    }

    public void setRequestKey(String requestKey) {
        this.requestKey = requestKey;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public void setRequestHash(String requestHash) {
        this.requestHash = requestHash;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getDetailAddress() {
        return detailAddress;
    }

    public void setDetailAddress(String detailAddress) {
        this.detailAddress = detailAddress;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
```

ShopOrder是数据库主表一行，不直接充当浏览器的下单请求或响应DTO。userId由服务端身份得到；totalAmount由服务端核算；六项地址是服务端查询本人地址后复制；requestHash由Java计算。后续Controller不能把客户端JSON直接绑定成这个实体再insert，也不能把实体原样返回泄露内部hash/userId。

## 4. 新建明细实体

文件：D:\ShopAgent-X\backend\src\main\java\com\wwzyhy\shopagent\entity\OrderItem.java

```java
package com.wwzyhy.shopagent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;

@TableName("order_items")
public class OrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("order_id")
    private Long orderId;

    @TableField("product_id")
    private Long productId;

    private String title;
    private String brand;

    @TableField("image_url")
    private String imageUrl;

    @TableField("unit_price")
    private BigDecimal unitPrice;

    private Integer quantity;

    @TableField("line_amount")
    private BigDecimal lineAmount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getLineAmount() {
        return lineAmount;
    }

    public void setLineAmount(BigDecimal lineAmount) {
        this.lineAmount = lineAmount;
    }
}
```

orderId把这行明细关联到一个订单，productId说明买的是哪个商品；两者含义不同。title/brand/unitPrice/imageUrl都是当时的快照。lineAmount由Java计算后存储，并由CHECK复核，不是前端报一个小计就信。

## 5. 新建两个Mapper

文件：D:\ShopAgent-X\backend\src\main\java\com\wwzyhy\shopagent\mapper\ShopOrderMapper.java

```java
package com.wwzyhy.shopagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wwzyhy.shopagent.entity.ShopOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ShopOrderMapper extends BaseMapper<ShopOrder> {
}
```

文件：D:\ShopAgent-X\backend\src\main\java\com\wwzyhy\shopagent\mapper\OrderItemMapper.java

```java
package com.wwzyhy.shopagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wwzyhy.shopagent.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {
}
```

继承BaseMapper只提供SQL能力，不自动检查用户归属。后续Service先查shop_orders.id+user_id确认本人，再按order_id查明细；不能给出一个明细编号就直接对外返回。订单号即使可猜测，也必须每次检查归属。

## 6. 新建022本地授权

文件：D:\ShopAgent-X\backend\sql\022_grant_orders.sql

```sql
USE shopagent;

GRANT SELECT, INSERT, UPDATE
    ON shopagent.shop_orders
    TO 'shopagent_app'@'127.0.0.1';

GRANT SELECT, INSERT
    ON shopagent.order_items
    TO 'shopagent_app'@'127.0.0.1';

SHOW GRANTS FOR 'shopagent_app'@'127.0.0.1';
```

在刚才本地管理连接手动执行。主表要更新状态，所以有UPDATE；明细创建后固定，只给SELECT/INSERT；不物理删订单，不授DELETE。实体上有setter、BaseMapper有delete方法，不等于应用账号就有数据库执行权限。

## 之后怎样接线与集中验收

下一组提供请求/响应DTO和结算预览：请求只收addressId、items(productId/quantity)，建单另加requestKey；不给客户端传userId、价格、总额或状态的入口。先实现后端现价与可售/库存预检，再原子建单/预占；预览不是库存承诺，建单必须在事务里重新检查。

建单/取消/过期/支付都要短事务，按固定商品编号顺序锁库存。取消/过期/支付先锁对应订单并判断状态，保证只释放或消费一次；支付外部HTTP不能放进持锁事务。后续原商品新增接口要同事务初始化库存0，不能忘记这一既有接口接线。

等相关Service/Controller接齐再集中核本地真实表、权限、金额/快照/归属、同键回放与异内容冲突、并发超卖、失败回滚、取消/过期释放及原功能回归。当前仅教案，不称订单、支付或过期任务已实现；未运行候选或正式测试。
