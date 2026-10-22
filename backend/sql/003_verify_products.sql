-- 只读查询，用于核对样例数据。
USE shopagent;

-- 查看全部商品；ORDER BY id 保证本次按编号显示。
SELECT id, title, brand, price, image_url, description, status
FROM products
ORDER BY id;

-- 预期总数 5，上架 4，下架 1。
SELECT COUNT(*) AS total,
       SUM(status = 1) AS on_sale,
       SUM(status = 0) AS off_sale
FROM products;

-- 只查上架商品，预期 4 条。
SELECT id, title, price, status
FROM products
WHERE status = 1
ORDER BY id;

-- 标题或品牌包含“跑鞋”的上架商品，预期 2 条。
SELECT id, title, price
FROM products
WHERE status = 1
  AND (title LIKE '%跑鞋%' OR brand LIKE '%跑鞋%')
ORDER BY id;
