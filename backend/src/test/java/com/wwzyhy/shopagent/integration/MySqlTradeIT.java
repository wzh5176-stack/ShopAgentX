package com.wwzyhy.shopagent.integration;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.spring.MybatisSqlSessionFactoryBean;
import com.wwzyhy.shopagent.dto.*;
import com.wwzyhy.shopagent.dto.AlipayQueryResult.TradeStatus;
import com.wwzyhy.shopagent.exception.CheckoutUnavailableException;
import com.wwzyhy.shopagent.mapper.*;
import com.wwzyhy.shopagent.service.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

/** 真实 MySQL、真实 Mapper 与业务方法；只有网关结果是合成数据。 */
class MySqlTradeIT {
    private static JdbcTemplate jdbc;
    private static TransactionTemplate tx;
    private static ProductInventoryMapper inventory;
    private static OrderWriteService writer;
    private static AlipayPaymentSettlementService settlement;
    private static OrderCloseService closer;
    private static final AtomicLong IDS = new AtomicLong(100);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    @BeforeAll static void initializeEmptyDatabase() throws Exception {
        assertEquals("empty-regression-db", System.getenv("SHOPAGENT_TEST_MYSQL_ALLOW"), "explicit test database opt-in required");
        String url = System.getenv("SHOPAGENT_TEST_MYSQL_URL");
        assertNotNull(url, "test database URL required");
        assertTrue(url.matches("jdbc:mysql://(127\\.0\\.0\\.1|localhost):[0-9]+/shopagent_regression(\\?.*)?"),
                "only a loopback shopagent_regression database is allowed");
        DataSource source = new DriverManagerDataSource(url, System.getenv("SHOPAGENT_TEST_MYSQL_USER"),
                System.getenv("SHOPAGENT_TEST_MYSQL_PASSWORD"));
        jdbc = new JdbcTemplate(source);
        assertEquals("shopagent_regression", jdbc.queryForObject("SELECT DATABASE()", String.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()", Integer.class),
                "database must be empty; no dropping/truncating an existing database");
        try (var connection = source.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new FileSystemResource("db/schema-baseline.sql"));
        }
        tx = new TransactionTemplate(new DataSourceTransactionManager(source));
        var configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addMapper(ProductInventoryMapper.class);
        configuration.addMapper(ProductMapper.class);
        configuration.addMapper(ShopOrderMapper.class);
        configuration.addMapper(OrderItemMapper.class);
        configuration.addMapper(UserAddressMapper.class);
        configuration.addMapper(PaymentAttemptMapper.class);
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(source);
        factory.setConfiguration(configuration);
        var session = new SqlSessionTemplate(Objects.requireNonNull(factory.getObject()));
        inventory = session.getMapper(ProductInventoryMapper.class);
        var orders = session.getMapper(ShopOrderMapper.class);
        var items = session.getMapper(OrderItemMapper.class);
        var payments = session.getMapper(PaymentAttemptMapper.class);
        writer = new OrderWriteService(orders, items, session.getMapper(ProductMapper.class),
                session.getMapper(UserAddressMapper.class), inventory, CLOCK);
        // 仅调用内部 reconcile；HTTP 身份边界由标准 Security/Service 测试验证。
        settlement = new AlipayPaymentSettlementService(null, payments, orders, items, inventory, CLOCK);
        closer = new OrderCloseService(null, orders, items, inventory, CLOCK, payments);
        jdbc.update("INSERT INTO app_users(id,username,password_hash) VALUES (1,'fixture_user','test-only'),(2,'other_user','test-only')");
        jdbc.update("""
                INSERT INTO user_addresses(id,user_id,recipient_name,phone,province,city,district,detail_address)
                VALUES(1,1,'测试收货人','00000000000','测试省','测试市','测试区','仅测试地址')
                """);
    }

    private long product(int available) {
        long id = IDS.incrementAndGet();
        jdbc.update("INSERT INTO products(id,title,brand,price,description) VALUES (?,'测试商品','Demo',19.90,'仅回归测试')", id);
        jdbc.update("INSERT INTO product_inventory(product_id,available_quantity,reserved_quantity) VALUES(?,?,0)", id, available);
        return id;
    }
    private OrderDetailResponse order(long... productIds) {
        var rows = Arrays.stream(productIds).sorted().mapToObj(id -> new CheckoutItemRequest(id, 1)).toList();
        var prepared = new PreparedOrderRequest(UUID.randomUUID().toString(), 1L, rows, "a".repeat(64));
        return tx.execute(status -> writer.create(1L, prepared));
    }
    private int stock(long id, String column) {
        assertTrue(Set.of("available_quantity", "reserved_quantity").contains(column));
        return jdbc.queryForObject("SELECT " + column + " FROM product_inventory WHERE product_id=?", Integer.class, id);
    }
    private long payment(OrderDetailResponse order) {
        long id = IDS.incrementAndGet();
        jdbc.update("""
                INSERT INTO payment_attempts(id,order_id,request_key,channel,status,amount,expires_at,created_at,updated_at,
                    out_trade_no,provider_app_id,provider_seller_id)
                VALUES(?,?,?,'ALIPAY_SANDBOX','PENDING',?,'2026-01-01 00:15:00','2026-01-01 00:00:00','2026-01-01 00:00:00',?,'test_app','test_seller')
                """, id, order.id(), UUID.randomUUID().toString(), new BigDecimal(order.totalAmount()), "test_trade_" + id);
        return id;
    }
    private AlipayQueryResult success(long id, OrderDetailResponse order) {
        return new AlipayQueryResult(id, order.id(), "test_trade_" + id, "test_app", "test_seller", TradeStatus.TRADE_SUCCESS,
                new BigDecimal(order.totalAmount()), "provider_" + id, LocalDateTime.now(CLOCK));
    }

    @Test void baselineCreatesAllTwelveTablesAndEnforcesConstraints() {
        assertEquals(13, jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()", Integer.class));
        long id = product(1);
        var rejected = assertThrows(org.springframework.dao.DataAccessException.class,
                () -> jdbc.update("UPDATE product_inventory SET available_quantity=-1 WHERE product_id=?", id));
        var cause = assertInstanceOf(java.sql.SQLException.class, rejected.getMostSpecificCause());
        assertEquals(3819, cause.getErrorCode());
        assertEquals(1, stock(id, "available_quantity"));
    }

    @Test void twentyConcurrentReservationsCannotOversellFiveItems() throws Exception {
        long id = product(5);
        var executor = Executors.newFixedThreadPool(20);
        var start = new CountDownLatch(1);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int index = 0; index < 20; index++) {
                futures.add(executor.submit(() -> { start.await(); return tx.execute(status -> inventory.reserve(id, 1)); }));
            }
            start.countDown();
            int accepted = 0;
            for (var future : futures) { accepted += future.get(20, TimeUnit.SECONDS); }
            assertEquals(5, accepted);
            assertEquals(0, stock(id, "available_quantity"));
            assertEquals(5, stock(id, "reserved_quantity"));
        } finally { executor.shutdownNow(); assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS)); }
    }

    @Test void secondSkuFailureRollsBackFirstReservationAndOrderRows() {
        long first = product(5), second = product(0);
        int before = jdbc.queryForObject("SELECT COUNT(*) FROM shop_orders", Integer.class);
        assertThrows(CheckoutUnavailableException.class, () -> order(first, second));
        assertEquals(5, stock(first, "available_quantity"));
        assertEquals(0, stock(first, "reserved_quantity"));
        assertEquals(before, jdbc.queryForObject("SELECT COUNT(*) FROM shop_orders", Integer.class));
    }

    @Test void duplicateUserRequestIsEnforcedByDatabaseUniqueConstraint() {
        long id = product(5);
        var prepared = new PreparedOrderRequest(UUID.randomUUID().toString(), 1L,
                List.of(new CheckoutItemRequest(id, 1)), "b".repeat(64));
        tx.execute(status -> writer.create(1L, prepared));
        assertThrows(org.springframework.dao.DuplicateKeyException.class,
                () -> tx.execute(status -> writer.create(1L, prepared)));
        assertEquals(4, stock(id, "available_quantity"));
        assertEquals(1, stock(id, "reserved_quantity"));
    }

    @Test void concurrentConfirmedResultsConsumeReservationOnlyOnce() throws Exception {
        long productId = product(5);
        var order = order(productId);
        long payment = payment(order);
        var executor = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            Callable<PaymentResponse> task = () -> { start.await(); return tx.execute(s -> settlement.reconcile(payment, success(payment, order))); };
            var first = executor.submit(task);
            var second = executor.submit(task);
            start.countDown();
            assertEquals(PaymentStatus.SUCCEEDED, first.get(20, TimeUnit.SECONDS).status());
            assertEquals(PaymentStatus.SUCCEEDED, second.get(20, TimeUnit.SECONDS).status());
        } finally { executor.shutdownNow(); assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS)); }
        assertEquals(4, stock(productId, "available_quantity"));
        assertEquals(0, stock(productId, "reserved_quantity"));
        assertEquals("PAID", jdbc.queryForObject("SELECT status FROM shop_orders WHERE id=?", String.class, order.id()));
    }

    @Test void partialSettlementFailureRollsBackFirstSkuAndBothStatuses() {
        long first = product(5), second = product(5);
        var order = order(first, second);
        long payment = payment(order);
        jdbc.update("UPDATE product_inventory SET reserved_quantity=0 WHERE product_id=?", second);
        assertThrows(IllegalStateException.class,
                () -> tx.execute(status -> settlement.reconcile(payment, success(payment, order))));
        assertEquals(1, stock(first, "reserved_quantity"));
        assertEquals("PENDING_PAYMENT", jdbc.queryForObject("SELECT status FROM shop_orders WHERE id=?", String.class, order.id()));
        assertEquals("PENDING", jdbc.queryForObject("SELECT status FROM payment_attempts WHERE id=?", String.class, payment));
    }

    @Test void expiryReleasesInventoryExactlyOnce() {
        long productId = product(5);
        var order = order(productId);
        jdbc.update("UPDATE shop_orders SET expires_at='2025-12-31 23:59:59',created_at='2025-12-31 23:00:00' WHERE id=?", order.id());
        assertTrue(Boolean.TRUE.equals(tx.execute(status -> closer.expire(order.id()))));
        assertFalse(Boolean.TRUE.equals(tx.execute(status -> closer.expire(order.id()))));
        assertEquals(5, stock(productId, "available_quantity"));
        assertEquals(0, stock(productId, "reserved_quantity"));
    }

    @Test void repeatedConcurrentReplenishmentAddsOnceAndKeepsReservations() throws Exception {
        long id = product(5);
        jdbc.update("UPDATE product_inventory SET reserved_quantity=2 WHERE product_id=?", id);
        var service = new InventoryService(jdbc);
        var request = new InventoryReplenishRequest(UUID.randomUUID().toString(), 7, "测试补货");
        var pool = Executors.newFixedThreadPool(8);
        try {
            List<Future<Long>> futures = new ArrayList<>();
            for (int i = 0; i < 16; i++) futures.add(pool.submit(() -> tx.execute(s -> service.replenish(id, request, "fixture_admin")).id()));
            Set<Long> ids = new HashSet<>();
            for (var future : futures) ids.add(future.get(15, TimeUnit.SECONDS));
            assertEquals(1, ids.size());
            assertEquals(12, stock(id, "available_quantity"));
            assertEquals(2, stock(id, "reserved_quantity"));
        } finally { pool.shutdownNow(); }
    }

    @Test void differentConcurrentReplenishmentsDoNotLoseUpdates() throws Exception {
        long id = product(5);
        var service = new InventoryService(jdbc);
        var pool = Executors.newFixedThreadPool(8);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 16; i++) futures.add(pool.submit(() -> tx.execute(s -> service.replenish(id,
                    new InventoryReplenishRequest(UUID.randomUUID().toString(), 2, "并发补货"), "fixture_admin"))));
            for (var future : futures) future.get(15, TimeUnit.SECONDS);
            assertEquals(37, stock(id, "available_quantity"));
            assertEquals(16, service.history(id).size());
        } finally { pool.shutdownNow(); }
    }

    @Test void replayConflictAndOverflowDoNotChangeStock() {
        long id = product(0);
        var service = new InventoryService(jdbc);
        String key = UUID.randomUUID().toString();
        tx.execute(s -> service.replenish(id, new InventoryReplenishRequest(key, 2, "初次补货"), "fixture_admin"));
        assertThrows(com.wwzyhy.shopagent.exception.InventoryConflictException.class, () -> tx.execute(s ->
                service.replenish(id, new InventoryReplenishRequest(key, 3, "初次补货"), "fixture_admin")));
        assertEquals(2, stock(id, "available_quantity"));
        jdbc.update("UPDATE product_inventory SET available_quantity=2147483647 WHERE product_id=?", id);
        assertThrows(com.wwzyhy.shopagent.exception.InventoryConflictException.class, () -> tx.execute(s ->
                service.replenish(id, new InventoryReplenishRequest(UUID.randomUUID().toString(), 1, "溢出测试"), "fixture_admin")));
        assertEquals(Integer.MAX_VALUE, stock(id, "available_quantity"));
        assertEquals(1, service.history(id).size());
    }

    @Test void auditInsertFailureRollsBackStockAddition() {
        long id = product(5);
        var service = new InventoryService(jdbc);
        jdbc.execute("ALTER TABLE inventory_replenishments ADD CONSTRAINT chk_fixture_audit_reason CHECK (reason <> '回滚测试')");
        try {
            assertThrows(org.springframework.dao.DataAccessException.class, () -> tx.execute(s -> service.replenish(id,
                    new InventoryReplenishRequest(UUID.randomUUID().toString(), 5, "回滚测试"), "fixture_admin")));
            assertEquals(5, stock(id, "available_quantity"));
            assertEquals(0, service.history(id).size());
        } finally { jdbc.execute("ALTER TABLE inventory_replenishments DROP CHECK chk_fixture_audit_reason"); }
    }

    @Test void recommendationFiltersNoStockBeforeCandidateLimit() throws Exception {
        String keyword = "库存筛选" + UUID.randomUUID().toString().substring(0, 8);
        for (int i = 0; i < 21; i++) {
            long id = product(0);
            jdbc.update("UPDATE products SET title=? WHERE id=?", keyword, id);
        }
        long purchasable = product(3);
        jdbc.update("UPDATE products SET title=? WHERE id=?", keyword, purchasable);
        // Use the real Mapper with MP's paging interceptor as the application does.
        var config = new MybatisConfiguration(); config.setMapUnderscoreToCamelCase(true); config.addMapper(ProductMapper.class);
        var factory = new MybatisSqlSessionFactoryBean(); factory.setDataSource(jdbc.getDataSource()); factory.setConfiguration(config);
        var plugin = new com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor();
        plugin.addInnerInterceptor(new com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor(com.baomidou.mybatisplus.annotation.DbType.MYSQL));
        factory.setPlugins(plugin);
        var mapper = new SqlSessionTemplate(Objects.requireNonNull(factory.getObject())).getMapper(ProductMapper.class);
        var products = new ProductService(mapper, inventory);
        assertEquals(List.of(purchasable), products.findCandidates(keyword, null).stream().map(com.wwzyhy.shopagent.entity.Product::getId).toList());
        assertTrue(products.findCandidates(keyword, new BigDecimal("1.00")).isEmpty());
        assertEquals(3, new InventoryService(jdbc).availableFor(List.of(purchasable)).get(purchasable));
    }
}
