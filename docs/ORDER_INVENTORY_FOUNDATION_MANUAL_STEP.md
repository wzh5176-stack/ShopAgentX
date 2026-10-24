# 阶段10第一组：为下单准备库存

2026-10-23：阶段9地址后端已集中验收，见[验收报告](ADDRESS_ACCEPTANCE.md)。现在开始订单与库存。以下是用户手写教案；助手没有代建这些正式SQL/Java文件，没有执行迁移或授权。

## 先明确这一阶段的实际请求流程

用户从购物车选择商品与收货地址 → Java复核本人地址、商品是否在售和数据库现价 → 一个数据库事务里创建订单/商品及地址快照、预占库存 → 返回待支付订单。

下单请求只提供商品编号/数量、地址编号和创建幂等键；金额、用户编号、订单状态由后端决定。购物车只是购买草稿，不是订单；历史订单不能随以后商品改价或用户改地址而变化。先做结算预览、建单，再本人列表/详情、取消和过期释放；阶段11才模拟支付。

本组只搭库存表、实体和Mapper，并给应用账号最小权限。没有下单接口，也不把库存预占单独暴露给浏览器。

## 1. 新建并手动执行019库存表

新建文件：D:\ShopAgent-X\backend\sql\019_create_product_inventory.sql

先在DataGrip连接Windows本地127.0.0.1:3306、shopagent，用有建表权限的原管理连接。先执行SELECT核目标，再单独执行后续建表段。不要在Linux库运行，也不要重跑001—018。若目标已存在product_inventory，先停下核对，不删表或重复初始化。

```sql
USE shopagent;

SELECT DATABASE(), VERSION(), CURRENT_USER(), @@hostname, @@port;
SHOW TABLES LIKE 'product_inventory';

CREATE TABLE product_inventory (
    product_id BIGINT NOT NULL,
    available_quantity INT NOT NULL DEFAULT 0,
    reserved_quantity INT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (product_id),
    CONSTRAINT fk_product_inventory_product
        FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_product_inventory_available
        CHECK (available_quantity >= 0),
    CONSTRAINT chk_product_inventory_reserved
        CHECK (reserved_quantity >= 0)
) ENGINE=InnoDB
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

INSERT INTO product_inventory (
    product_id, available_quantity, reserved_quantity
)
SELECT id,
       CASE WHEN status = 1 AND deleted = 0 THEN 20 ELSE 0 END,
       0
FROM products;

SELECT i.product_id, p.title, p.status, p.deleted,
       i.available_quantity, i.reserved_quantity
FROM product_inventory i
JOIN products p ON p.id = i.product_id
ORDER BY i.product_id;
```

这里是虚拟演示库存：现有未删除的在售商品每种20件，其余0件。只初始化新表，不改旧products数据，不代表真实仓库。INSERT SELECT是把查询到的每件商品各生成一行库存，不是让你手动抄商品编号。若CREATE成功而INSERT失败，不要重跑整份脚本；保留错误，先核库存表是否已写入。MySQL建表不会随普通事务完整回滚。

product_id同时是主键和外键：一件商品只有一行库存，库存必须引用真实商品。它沿用商品编号，不再额外生成另一个编号。

available_quantity是还可以卖的数量，reserved_quantity是待支付订单占住的数量。例如20/0 → 下单买2件后18/2；取消回到20/0；支付成功变18/0。支付成功后的2件已卖出，不能再返回可售库存。后续三种变更都必须检查订单状态并在事务里执行，不能让前端随意修改这两个数。

## 2. 新建实体ProductInventory

文件：D:\ShopAgent-X\backend\src\main\java\com\wwzyhy\shopagent\entity\ProductInventory.java

```java
package com.wwzyhy.shopagent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("product_inventory")
public class ProductInventory {

    @TableId(value = "product_id", type = IdType.INPUT)
    private Long productId;

    @TableField("available_quantity")
    private Integer availableQuantity;

    @TableField("reserved_quantity")
    private Integer reservedQuantity;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public Integer getReservedQuantity() {
        return reservedQuantity;
    }

    public void setReservedQuantity(Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
```

TableName对应SQL表；TableField明确Java字段对应哪一列。IdType.INPUT表示主键由业务提供：例如商品id为5，它的库存productId也为5。SQL初始化时已经提供了这个值。与地址表id自增AUTO不同，这里不能生成一个与商品无关的新编号。

Long对应BIGINT，Integer对应INT，LocalDateTime对应DATETIME。实体只是承载数据库行，不是浏览器提交的库存修改请求。

## 3. 新建库存Mapper

文件：D:\ShopAgent-X\backend\src\main\java\com\wwzyhy\shopagent\mapper\ProductInventoryMapper.java

```java
package com.wwzyhy.shopagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wwzyhy.shopagent.entity.ProductInventory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductInventoryMapper
        extends BaseMapper<ProductInventory> {
}
```

它让MyBatis-Plus知道操作product_inventory、每行映射成ProductInventory。以后selectById(5L)查询的是product_id=5。此处先保留基础能力，真正扣减要在订单Service中使用锁定读取和有数量条件的SQL，不能先查一个数字再无条件覆盖，否则并发会超卖。

## 4. 新建并手动执行020授权

文件：D:\ShopAgent-X\backend\sql\020_grant_product_inventory.sql

```sql
USE shopagent;

GRANT SELECT, INSERT, UPDATE
    ON shopagent.product_inventory
    TO 'shopagent_app'@'127.0.0.1';

SHOW GRANTS FOR 'shopagent_app'@'127.0.0.1';
```

在刚才本地管理连接执行。应用运行身份已经在地址验收中确认是这一账号；仅给新表读、新增、修改，不给建表或删除权限。INSERT用于后续创建商品时同时初始化0库存，目前ProductService.create尚未接线；本阶段后续必须补齐，不能说新增商品已经有库存。库存行缺失时下单必须视为不可售，不得当作无限库存。

本组写完暂不逐文件验收。下一组继续订单表与明细表、地址/商品快照及创建幂等约定，再接Service/Controller，几个相关步骤后集中检查。页面、Python和官方沙箱这一步均不用动。
