# 阶段10第五组：事务建单与事务外幂等协调

日期：2026-10-23。先按用户明确授权恢复缺失021/022，并只读确认本机真实表/权限已经正确，无需重执行SQL，见[SQL恢复记录](ORDER_SQL_RECOVERY.md)。接下来的6个新Java文件仍由用户手写；助手本组只提供完整代码与解析，不代写这些应用文件，不编译或运行候选/正式业务。没有实际创建订单或扣库存。

## 本组链路和边界

后续HTTP入口将调用OrderCreateService.create(authentication,request)：准备规范化请求→取当前有效用户ID→先查同键旧单→若无，调用独立OrderWriteService事务新建→事务提交成功后返回OrderCreateResult；若唯一键、锁竞争或可售条件变化失败，在已回滚的事务外重新找同键旧单，存在则校验摘要并回放。OrderReadService读操作每次独立事务，避免失败事务或旧RR快照污染回查。

真正写入时：本人地址FOR SHARE→按商品编号顺序FOR SHARE锁真实在售未删商品并重新核价→先INSERT订单主表占住每用户请求唯一键→按相同商品顺序reserve库存并INSERT明细→当前读回查并组装详情→代理提交。后续同键竞争失败不会重复占库存；中途任何异常全部回滚。这个方法内部不调用CheckoutService.preview，不信客户端或旧预览金额，也不调用支付/模型。

本组实现Service业务核心，还没有OrderController/订单Security规则/专用Advice。下一组统一接POST创建＋GET详情和异常/权限，再分页、取消/过期释放与商品创建库存初始化，最终集中验收。

## 1. 新建订单不存在异常

文件：D:\ShopAgent-X\backend\src\main\java\com\wwzyhy\shopagent\exception\OrderNotFoundException.java

```java
package com.wwzyhy.shopagent.exception;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException() {
        super("订单不存在");
    }
}
```

后续统一映射404 ORDER_NOT_FOUND，他人的订单与不存在订单同样返回404，不泄露归属。当前尚无对应订单HTTP Advice，不要在只写完本组时请求测试。

## 2. 新建订单冲突异常

文件：D:\ShopAgent-X\backend\src\main\java\com\wwzyhy\shopagent\exception\OrderConflictException.java

```java
package com.wwzyhy.shopagent.exception;

public class OrderConflictException extends RuntimeException {

    public OrderConflictException(String message) {
        super(message);
    }
}
```

本组用于同一requestKey换了购买内容，后续映射409 ORDER_CONFLICTED。库存/商品不可用继续用已写CheckoutUnavailableException，订单Advice下一组同时映射，不改全局其他模块含义。

## 3. 新建内部创建结果

文件：D:\ShopAgent-X\backend\src\main\java\com\wwzyhy\shopagent\dto\OrderCreateResult.java

```java
package com.wwzyhy.shopagent.dto;

public record OrderCreateResult(
        boolean created,
        OrderDetailResponse data
) {
}
```

created为true表示本次新建，false表示回放旧单；data始终完整订单详情。后续Controller据此首次201、回放200，两者Location都指向/api/orders/{id}，HTTP正文只包data，不直接输出此内部标记。

## 4. 新建独立订单读取Service

文件：D:\ShopAgent-X\backend\src\main\java\com\wwzyhy\shopagent\service\OrderReadService.java

```java
package com.wwzyhy.shopagent.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wwzyhy.shopagent.dto.OrderDetailResponse;
import com.wwzyhy.shopagent.dto.PreparedOrderRequest;
import com.wwzyhy.shopagent.entity.OrderItem;
import com.wwzyhy.shopagent.entity.ShopOrder;
import com.wwzyhy.shopagent.exception.OrderConflictException;
import com.wwzyhy.shopagent.exception.OrderNotFoundException;
import com.wwzyhy.shopagent.mapper.OrderItemMapper;
import com.wwzyhy.shopagent.mapper.ShopOrderMapper;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderReadService {

    private final CurrentUserService currentUserService;
    private final ShopOrderMapper orderMapper;
    private final OrderItemMapper itemMapper;

    public OrderReadService(
            CurrentUserService currentUserService,
            ShopOrderMapper orderMapper,
            OrderItemMapper itemMapper) {
        this.currentUserService = currentUserService;
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public OrderDetailResponse findByRequest(
            Long userId,
            PreparedOrderRequest request) {
        QueryWrapper<ShopOrder> query = new QueryWrapper<>();
        query.eq("user_id", userId);
        query.eq("request_key", request.requestKey());

        ShopOrder order = orderMapper.selectOne(query);

        if (order == null) {
            return null;
        }

        if (!request.requestHash().equals(order.getRequestHash())) {
            throw new OrderConflictException(
                    "同一请求编号不能用于不同的下单内容");
        }

        return toResponse(order);
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public OrderDetailResponse get(
            Authentication authentication,
            Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("订单编号必须为正整数");
        }

        Long userId = currentUserService.requireUserId(authentication);

        QueryWrapper<ShopOrder> query = new QueryWrapper<>();
        query.eq("id", id);
        query.eq("user_id", userId);

        ShopOrder order = orderMapper.selectOne(query);

        if (order == null) {
            throw new OrderNotFoundException();
        }

        return toResponse(order);
    }

    private OrderDetailResponse toResponse(ShopOrder order) {
        QueryWrapper<OrderItem> query = new QueryWrapper<>();
        query.eq("order_id", order.getId());
        query.orderByAsc("id");

        List<OrderItem> rows = itemMapper.selectList(query);

        if (rows.isEmpty()) {
            throw new IllegalStateException("订单明细缺失");
        }

        return OrderDetailResponse.from(order, rows);
    }
}
```

findByRequest用user_id+request_key查旧单，不查询当前价格、地址或库存；旧单已取消/过期/支付也回放其保存状态。同键异hash抛业务冲突。get以当前用户ID+订单ID检查归属，再查该订单明细，不能直接按明细ID对外查询。

REQUIRES_NEW在这里表示每次独立读事务；失败建单结束后回查，不沿用旧失败事务或它的RR快照。主表与明细在这个读事务里取一致视图，明细为购买快照，不查当前商品；读取不自动改变过期状态。写事务中不要调用此独立读取服务读取自己尚未提交的记录，写后回查见下一类内部方法。

## 5. 新建事务写订单Service

文件：D:\ShopAgent-X\backend\src\main\java\com\wwzyhy\shopagent\service\OrderWriteService.java

```java
package com.wwzyhy.shopagent.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wwzyhy.shopagent.dto.CheckoutItemRequest;
import com.wwzyhy.shopagent.dto.OrderDetailResponse;
import com.wwzyhy.shopagent.dto.OrderStatus;
import com.wwzyhy.shopagent.dto.PreparedOrderRequest;
import com.wwzyhy.shopagent.entity.OrderItem;
import com.wwzyhy.shopagent.entity.Product;
import com.wwzyhy.shopagent.entity.ShopOrder;
import com.wwzyhy.shopagent.entity.UserAddress;
import com.wwzyhy.shopagent.exception.AddressNotFoundException;
import com.wwzyhy.shopagent.exception.CheckoutUnavailableException;
import com.wwzyhy.shopagent.mapper.OrderItemMapper;
import com.wwzyhy.shopagent.mapper.ProductInventoryMapper;
import com.wwzyhy.shopagent.mapper.ProductMapper;
import com.wwzyhy.shopagent.mapper.ShopOrderMapper;
import com.wwzyhy.shopagent.mapper.UserAddressMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderWriteService {

    private final ShopOrderMapper orderMapper;
    private final OrderItemMapper itemMapper;
    private final ProductMapper productMapper;
    private final UserAddressMapper addressMapper;
    private final ProductInventoryMapper inventoryMapper;
    private final Clock clock;

    public OrderWriteService(
            ShopOrderMapper orderMapper,
            OrderItemMapper itemMapper,
            ProductMapper productMapper,
            UserAddressMapper addressMapper,
            ProductInventoryMapper inventoryMapper,
            Clock clock) {
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
        this.productMapper = productMapper;
        this.addressMapper = addressMapper;
        this.inventoryMapper = inventoryMapper;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OrderDetailResponse create(
            Long userId,
            PreparedOrderRequest request) {
        UserAddress address = lockAddress(userId, request.addressId());

        List<OrderItem> rows = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CheckoutItemRequest item : request.items()) {
            Product product = lockProduct(item.productId());

            BigDecimal lineAmount = product.getPrice().multiply(
                    BigDecimal.valueOf(item.quantity()));

            OrderItem row = new OrderItem();
            row.setProductId(product.getId());
            row.setTitle(product.getTitle());
            row.setBrand(product.getBrand());
            row.setImageUrl(product.getImageUrl());
            row.setUnitPrice(product.getPrice());
            row.setQuantity(item.quantity());
            row.setLineAmount(lineAmount);

            rows.add(row);
            totalAmount = totalAmount.add(lineAmount);
        }

        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CheckoutUnavailableException(
                    "本轮暂不支持总金额为0的订单");
        }

        LocalDateTime now = LocalDateTime.now(clock)
                .truncatedTo(ChronoUnit.MILLIS);

        ShopOrder order = new ShopOrder();
        order.setUserId(userId);
        order.setRequestKey(request.requestKey());
        order.setRequestHash(request.requestHash());
        order.setStatus(OrderStatus.PENDING_PAYMENT.name());
        order.setTotalAmount(totalAmount);
        order.setRecipientName(address.getRecipientName());
        order.setPhone(address.getPhone());
        order.setProvince(address.getProvince());
        order.setCity(address.getCity());
        order.setDistrict(address.getDistrict());
        order.setDetailAddress(address.getDetailAddress());
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        order.setExpiresAt(now.plusMinutes(15));

        if (orderMapper.insert(order) != 1 || order.getId() == null) {
            throw new IllegalStateException("创建订单失败");
        }

        for (OrderItem row : rows) {
            int reserved = inventoryMapper.reserve(
                    row.getProductId(), row.getQuantity());

            if (reserved != 1) {
                throw new CheckoutUnavailableException(
                        "商品库存不足，请调整购买数量后重试");
            }

            row.setOrderId(order.getId());

            if (itemMapper.insert(row) != 1 || row.getId() == null) {
                throw new IllegalStateException("保存订单明细失败");
            }
        }

        if (!LocalDateTime.now(clock).isBefore(order.getExpiresAt())) {
            throw new CheckoutUnavailableException(
                    "创建订单耗时过长，请重试");
        }

        return readSaved(userId, order.getId(), rows.size());
    }

    private UserAddress lockAddress(Long userId, Long addressId) {
        QueryWrapper<UserAddress> query = new QueryWrapper<>();
        query.eq("id", addressId);
        query.eq("user_id", userId);
        query.last("FOR SHARE");

        UserAddress address = addressMapper.selectOne(query);

        if (address == null) {
            throw new AddressNotFoundException();
        }

        return address;
    }

    private Product lockProduct(Long productId) {
        QueryWrapper<Product> query = new QueryWrapper<>();
        query.eq("id", productId);
        query.eq("status", 1);
        query.last("FOR SHARE");

        Product product = productMapper.selectOne(query);

        if (product == null) {
            throw new CheckoutUnavailableException(
                    "部分商品不存在或已下架，请刷新后重选");
        }

        return product;
    }

    private OrderDetailResponse readSaved(
            Long userId,
            Long orderId,
            int expectedItems) {
        QueryWrapper<ShopOrder> orderQuery = new QueryWrapper<>();
        orderQuery.eq("id", orderId);
        orderQuery.eq("user_id", userId);
        orderQuery.last("FOR SHARE");

        ShopOrder order = orderMapper.selectOne(orderQuery);

        QueryWrapper<OrderItem> itemQuery = new QueryWrapper<>();
        itemQuery.eq("order_id", orderId);
        itemQuery.orderByAsc("id");
        itemQuery.last("FOR SHARE");

        List<OrderItem> rows = itemMapper.selectList(itemQuery);

        if (order == null || rows.size() != expectedItems) {
            throw new IllegalStateException("读取保存后的订单失败");
        }

        return OrderDetailResponse.from(order, rows);
    }
}
```

### 重要代码解释

这个类只供后续外层OrderCreateService调用，参数userId必须来自可信CurrentUserService，PreparedOrderRequest必须经上组OrderRequestService准备，不允许Controller直接接用户自报userId/hash。它不重复排序，沿用已验证且不可变的按商品编号排序列表。

FOR SHARE是当前锁定读：取得的地址/商品在事务结束前不能被其他事务修改或删除，保证复制快照和核价期间不被改掉；商品可被其他正常读请求同时读取。Product带TableLogic，MyBatis-Plus继续自动加deleted=0。这里用新锁定查询而不是旧预览金额；库存则由reserve条件UPDATE取得行锁并原子预占。

先准备商品行和总额，insert主表后得到数据库回填id；此时还未提交。每用户请求唯一键在库存更新之前争用，重复键失败者不会先重复占一遍库存。然后按相同商品顺序预占和写明细，任一步非1或读取失败都抛RuntimeException，由事务代理全部回滚。已有订单表/明细/库存均InnoDB且在同一个DataSource，才能一起回滚。

setOrderId把明细绑定到刚生成的订单id；不是商品id。setRecipientName等从锁定的本人地址复制，商品名称/价格等从锁定的真实商品复制。Clock复用现有Bean，毫秒精度对应DATETIME(3)，15分钟从createdAt计算；很长锁等待导致创建已经超时会失败回滚。

readSaved在当前写事务内部使用当前读查看自己的已保存数据，不调用独立OrderReadService。方法返回后Spring事务代理仍要提交，提交成功后外层调用方才拿到返回值。故“走到return”不能提前对浏览器报告创建成功。

## 6. 新建外层创建协调Service

文件：D:\ShopAgent-X\backend\src\main\java\com\wwzyhy\shopagent\service\OrderCreateService.java

```java
package com.wwzyhy.shopagent.service;

import com.wwzyhy.shopagent.dto.OrderCreateRequest;
import com.wwzyhy.shopagent.dto.OrderCreateResult;
import com.wwzyhy.shopagent.dto.OrderDetailResponse;
import com.wwzyhy.shopagent.dto.PreparedOrderRequest;
import com.wwzyhy.shopagent.exception.AddressNotFoundException;
import com.wwzyhy.shopagent.exception.CheckoutUnavailableException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class OrderCreateService {

    private final OrderRequestService requestService;
    private final CurrentUserService currentUserService;
    private final OrderReadService readService;
    private final OrderWriteService writeService;

    public OrderCreateService(
            OrderRequestService requestService,
            CurrentUserService currentUserService,
            OrderReadService readService,
            OrderWriteService writeService) {
        this.requestService = requestService;
        this.currentUserService = currentUserService;
        this.readService = readService;
        this.writeService = writeService;
    }

    public OrderCreateResult create(
            Authentication authentication,
            OrderCreateRequest request) {
        PreparedOrderRequest prepared = requestService.prepare(request);
        Long userId = currentUserService.requireUserId(authentication);

        OrderDetailResponse existing =
                readService.findByRequest(userId, prepared);

        if (existing != null) {
            return new OrderCreateResult(false, existing);
        }

        try {
            OrderDetailResponse data = writeService.create(userId, prepared);
            return new OrderCreateResult(true, data);
        } catch (DuplicateKeyException
                 | PessimisticLockingFailureException
                 | CheckoutUnavailableException
                 | AddressNotFoundException exception) {
            existing = readService.findByRequest(userId, prepared);

            if (existing != null) {
                return new OrderCreateResult(false, existing);
            }

            throw exception;
        }
    }
}
```

这个协调类故意不加Transactional；独立读/写Bean各自的REQUIRES_NEW明确隔开事务。它准备好请求并取得可信用户ID，先回放旧单；旧单回放不重新查询当前地址、商品或库存。

两个相同请求可能同时第一查都没有旧单。一个写成功，另一个受唯一键或锁竞争影响失败；失败者等自己的写事务已经由代理结束后，再独立读取旧单。如果查询到内容一致的旧单就返回created=false；内容不一致由readService抛OrderConflictException；仍没有旧单则原异常上抛，后续Advice提示409/404等，由客户端保留原键重试，不假装成功。

同时回查AddressNotFound/CheckoutUnavailable是为了覆盖第一查旧单为空之后，另一个同键请求刚完成建单、随后商品/地址/库存发生变化的窗口；本请求失败时若已有同内容旧单应回放它。如果真正没有旧单，才返回本次不可售/地址错误。

不捕获全部Exception掩盖编程/数据错误，不在失败事务内部catch后继续写；读写分成不同Spring Bean，避免同类this.create绕过Transactional代理。真实HTTP与并发/回滚验证尚未执行，不能用源码逻辑当已验收证据。

## 本组写完后的正常/重试路径

假设一次合法请求创建订单100：外层返回OrderCreateResult(true,订单100详情)。同一用户同键同内容重试：返回OrderCreateResult(false,订单100详情)，不会再次调用库存reserve。同一键换数量：OrderConflictException。内部created不直接作为页面字段；下一组Controller决定HTTP201或200，并统一返回{data:订单详情}。

库存不足或者第二件商品处理失败：新主表、前面已写的明细和预占全部回滚；这一次键未成功占用，库存恢复。取消/超时释放尚未写，所以目前不让用户直接下单演示囤住库存；先接下组HTTP/异常/权限，再相关取消/过期组，最后集中真实验收。
