from copy import deepcopy
import json
from pathlib import Path
import sys
import tempfile
import unittest
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from import_demo_catalog import load_catalog


class DemoCatalogValidationTests(unittest.TestCase):
    def test_catalog_contains_36_unique_labeled_products_and_illustrations(self):
        catalog = load_catalog()
        self.assertEqual(len(catalog['products']), 36)
        self.assertEqual(len({p['key'] for p in catalog['products']}), 36)
        self.assertEqual(len({p['category'] for p in catalog['products']}), 6)
        self.assertEqual(sum(p['initialStock'] for p in catalog['products']), 720)

    def test_invalid_import_data_is_rejected_before_database_connection(self):
        for field, bad in [('price', '-1.00'), ('initialStock', -1), ('imageUrl', '/secrets/key'),
                           ('brand', '真实品牌'), ('description', '没有演示标记')]:
            catalog = deepcopy(load_catalog())
            catalog['products'][0][field] = bad
            with tempfile.TemporaryDirectory() as folder:
                path = Path(folder, 'bad.json')
                path.write_text(json.dumps(catalog), encoding='utf-8')
                with self.assertRaises(ValueError):
                    load_catalog(path)

    def test_repeated_product_identity_is_rejected(self):
        for duplicate_key_only in (False, True):
            catalog = deepcopy(load_catalog())
            if duplicate_key_only:
                catalog['products'][1]['key'] = catalog['products'][0]['key']
            else:
                catalog['products'].append(catalog['products'][0])
            with tempfile.TemporaryDirectory() as folder:
                path = Path(folder, 'duplicate.json')
                path.write_text(json.dumps(catalog), encoding='utf-8')
                with self.assertRaises(ValueError):
                    load_catalog(path)
