"""在Java集成测试建立的专用回归库中验证导入；不接触业务库。"""
from copy import deepcopy
from urllib.parse import urlsplit
import os
import unittest
from import_demo_catalog import connect, import_catalog, load_catalog


class CatalogMySqlIT(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        if os.environ.get('SHOPAGENT_TEST_MYSQL_ALLOW') != 'empty-regression-db':
            raise RuntimeError('explicit isolated database required')
        url = urlsplit(os.environ['SHOPAGENT_TEST_MYSQL_URL'].removeprefix('jdbc:'))
        if url.hostname not in {'127.0.0.1', 'localhost'} or url.path != '/shopagent_regression':
            raise RuntimeError('test target must be loopback shopagent_regression')
        cls.db = connect(url.hostname, url.port, 'shopagent_regression',
                         os.environ['SHOPAGENT_TEST_MYSQL_USER'], os.environ['SHOPAGENT_TEST_MYSQL_PASSWORD'])
        cls.catalog = load_catalog()

    @classmethod
    def tearDownClass(cls):
        cls.db.close()

    def test_01_preview_then_import_is_replay_safe_and_preserves_consumed_stock(self):
        with self.db.cursor() as c:
            c.execute('SELECT COUNT(*) FROM products')
            baseline = c.fetchone()[0]
        plan = import_catalog(self.db, self.catalog)
        self.assertEqual(plan['would_insert'], 36)
        with self.db.cursor() as c:
            c.execute('SELECT COUNT(*) FROM products')
            self.assertEqual(c.fetchone()[0], baseline)
        result = import_catalog(self.db, self.catalog, True)
        self.assertEqual(result['new_products'], 36)
        with self.db.cursor() as c:
            p = self.catalog['products'][0]
            c.execute('SELECT id FROM products WHERE title=%s AND brand=%s', (p['title'], p['brand']))
            product_id = c.fetchone()[0]
            c.execute('UPDATE product_inventory SET available_quantity=17,reserved_quantity=2 WHERE product_id=%s', (product_id,))
            c.execute('UPDATE products SET status=0,price=88.00 WHERE id=%s', (product_id,))
        self.db.commit()
        replay = import_catalog(self.db, self.catalog, True)
        self.assertEqual((replay['new_products'], replay['preserved_existing']), (0, 36))
        with self.db.cursor() as c:
            c.execute('SELECT available_quantity,reserved_quantity FROM product_inventory WHERE product_id=%s', (product_id,))
            self.assertEqual(c.fetchone(), (17, 2))
            c.execute('SELECT status,price FROM products WHERE id=%s', (product_id,))
            self.assertEqual(str(c.fetchone()[1]), '88.00')

    def test_02_late_validation_failure_rolls_back_earlier_insert(self):
        data = deepcopy(self.catalog)
        data['products'] = data['products'][:2]
        data['products'][0]['title'] = '事务回滚专用新商品'
        broken = data['products'][1]
        with self.db.cursor() as c:
            c.execute('SELECT id FROM products WHERE title=%s AND brand=%s', (broken['title'], broken['brand']))
            product_id = c.fetchone()[0]
            c.execute('DELETE FROM product_inventory WHERE product_id=%s', (product_id,))
        self.db.commit()
        with self.assertRaises(ValueError):
            import_catalog(self.db, data, True)
        with self.db.cursor() as c:
            c.execute('SELECT COUNT(*) FROM products WHERE title=%s', (data['products'][0]['title'],))
            self.assertEqual(c.fetchone()[0], 0)


if __name__ == '__main__':
    unittest.main(verbosity=2)
