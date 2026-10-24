# 阶段11第二组：支付实体、Mapper与请求响应（手写）

2026-10-24。上一组正式023/024 SQL文件及PaymentStatus已只读看到，不能据此认定数据库SQL已经执行。本组五个Java文件由用户手写；助手只维护教案/状态，不代建源、执行SQL、编译或宣称支付接口可用。几个相关步骤后集中检查。

## 本组的数据路径

未来浏览器JSON（orderId/requestKey）→PaymentCreateRequest→Service读取本人订单/计算支付数据→PaymentAttempt实体→PaymentAttemptMapper写表→PaymentResponse→Controller包data返回。HTTP入口和Service本组还不写，下面请求响应仅合同示例。

## 1. PaymentAttempt.java

新建D:\ShopAgent-X\backend\src\main\java\com\wwzyhy\shopagent\entity\PaymentAttempt.java：

```java
package com.wwzyhy.shopagent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("payment_attempts")
public class PaymentAttempt {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("order_id")
    private Long orderId;

    @TableField("request_key")
    private String requestKey;

    private String channel;
    private String status;
    private BigDecimal amount;

    @TableField("expires_at")
    private LocalDateTime expiresAt;

    @TableField("finished_at")
    private LocalDateTime finishedAt;

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

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getRequestKey() {
        return requestKey;
    }

    public void setRequestKey(String requestKey) {
        this.requestKey = requestKey;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
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

@TableName指定表；@TableId AUTO表示数据库自增；@TableField把Java的orderId映射为SQL的order_id。status沿用现有ShopOrder的String存储风格，后续用PaymentStatus.PENDING.name()设置；amount用BigDecimal精确计算，时间沿用LocalDateTime。

get读当前对象字段，set给当前对象赋值，都不是SQL。MyBatis-Plus通过Mapper读写对象。两个数据库生成列active_order_id/successful_order_id不加进实体，避免Java尝试写入。user_id也不添加，因为本表没有该列；后续通过所属订单检查登录用户，不能直接selectById后就对外返回。

## 2. PaymentAttemptMapper.java

新建D:\ShopAgent-X\backend\src\main\java\com\wwzyhy\shopagent\mapper\PaymentAttemptMapper.java：

```java
package com.wwzyhy.shopagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wwzyhy.shopagent.entity.PaymentAttempt;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentAttemptMapper
        extends BaseMapper<PaymentAttempt> {
}
```

extends继承，BaseMapper<PaymentAttempt>提供这个实体的insert/selectById/selectList/update等方法；不用手写每句基础SQL。@Mapper让框架创建可注入的Mapper实现。Mapper负责数据库操作，不负责归属、金额可信、订单状态等业务规则。新建接口本身不会插入任何数据。

## 3. PaymentCreateRequest.java

新建D:\ShopAgent-X\backend\src\main\java\com\wwzyhy\shopagent\dto\PaymentCreateRequest.java：

```java
package com.wwzyhy.shopagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record PaymentCreateRequest(
        @NotNull(message = "订单编号不能为空")
        @Positive(message = "订单编号必须为正整数")
        Long orderId,

        @NotBlank(message = "支付请求编号不能为空")
        @Pattern(
                regexp = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}",
                message = "支付请求编号必须是标准小写UUID")
        String requestKey
) {
}
```

示例请求（不是本轮已开放API）：

```json
{
  "orderId": 100,
  "requestKey": "b6209e80-970f-4f62-a810-72d4be7aefb6"
}
```

只需告诉后端为哪个订单付款，以及本次创建请求键。金额由Java读取订单；userId来自登录身份；channel当前由后端固定MOCK；付款结果不能随创建请求自报。不提交addressId/items，因为这些已在订单里。

record自动提供构造器与orderId()/requestKey()读取方法，不需要get/set。@NotNull防null，@Positive限制>0，@NotBlank防空白，@Pattern限制UUID格式。它们需要后续Controller的@Valid或Service Validator触发，不是new对象就自动检查；编号有效不代表订单属于本人。

同一次创建支付请求重试沿用这个支付requestKey；真正开启新的付款尝试才换键。它与建订单时的requestKey属于不同操作，不混用。

## 4. PaymentResponse.java

新建D:\ShopAgent-X\backend\src\main\java\com\wwzyhy\shopagent\dto\PaymentResponse.java：

```java
package com.wwzyhy.shopagent.dto;

import com.wwzyhy.shopagent.entity.PaymentAttempt;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long orderId,
        String channel,
        PaymentStatus status,
        String amount,
        LocalDateTime expiresAt,
        LocalDateTime finishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PaymentResponse from(PaymentAttempt payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getChannel(),
                PaymentStatus.valueOf(payment.getStatus()),
                payment.getAmount()
                        .setScale(2, RoundingMode.UNNECESSARY)
                        .toPlainString(),
                payment.getExpiresAt(),
                payment.getFinishedAt(),
                payment.getCreatedAt(),
                payment.getUpdatedAt());
    }
}
```

from是项目自己写的转换方法，名字不代表框架会自动调用。后续Service读取数据库实体后显式调用PaymentResponse.from(payment)。static允许用类名调用。

PaymentStatus.valueOf把数据库String转为枚举，例如"PENDING"→PaymentStatus.PENDING；Spring之后会把枚举序列化为JSON字符串。setScale(2, UNNECESSARY)要求金额能无舍入地表示为两位小数；toPlainString得到"598.00"。DTO不暴露requestKey与数据库生成列。

## 5. PaymentCreateResult.java

新建D:\ShopAgent-X\backend\src\main\java\com\wwzyhy\shopagent\dto\PaymentCreateResult.java：

```java
package com.wwzyhy.shopagent.dto;

public record PaymentCreateResult(
        boolean created,
        PaymentResponse data
) {
}
```

内部结果用法与OrderCreateResult一致：created=true表示本次新建记录，false表示回放/返回已有记录。data才是给浏览器看的支付内容。Controller后续根据created决定首次201或回放200，正文只包Map.of("data", result.data())，不把created整个输出。

示例完整HTTP正文（没有实际发请求，编号/金额/时间均演示）：

```json
{
  "data": {
    "id": 1,
    "orderId": 100,
    "channel": "MOCK",
    "status": "PENDING",
    "amount": "598.00",
    "expiresAt": "2026-10-24T15:15:00",
    "finishedAt": null,
    "createdAt": "2026-10-24T15:01:00",
    "updatedAt": "2026-10-24T15:01:00"
  }
}
```

层次是：PaymentAttempt数据库对象→PaymentResponse对外数据→PaymentCreateResult内部新建标记和数据→Controller仅将其中data包成完整HTTP正文。

## 本组结束与下一步

写完五个文件即可，不必为“尚未使用”黄色提示补调用，也不用重复执行建表SQL。当前仍无支付Controller/Service，不能测试付款。下一组创建/读取支付单Service、业务异常，再HTTP入口/精确权限；金额从订单读取、归属核对、统一先锁订单、同键回放、不同键已有待处理的精确行为会在Service课落实，然后再模拟结果。正式数据库表/权限和这组代码留集中验收，不逐文件启动检查。
