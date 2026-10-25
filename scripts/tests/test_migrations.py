import json
from pathlib import Path
import tempfile
import unittest
import shutil
from scripts.check_migrations import ROOT, checksum, split_sql, validate


class MigrationTests(unittest.TestCase):
    def test_repository_catalog_and_baseline_are_consistent(self):
        self.assertEqual(validate()["baseline_tables"], 13)

    def test_splitter_preserves_semicolons_inside_literals(self):
        self.assertEqual(split_sql("-- header\nINSERT INTO t VALUES ('a;b'); SELECT 'it''s';"),
                         ["INSERT INTO t VALUES ('a;b')", "SELECT 'it''s'"])

    def test_unsupported_or_incomplete_sql_is_rejected(self):
        for text in ("DELIMITER $$", "SELECT 'unfinished;", "SELECT 1", "/* unfinished"):
            with self.subTest(text=text), self.assertRaises(ValueError):
                split_sql(text)

    def test_checksum_is_portable_between_crlf_and_lf(self):
        with tempfile.TemporaryDirectory(prefix="shopagent-checksum-") as directory:
            first, second = Path(directory) / "a", Path(directory) / "b"
            first.write_bytes(b"SELECT 1;\r\n")
            second.write_bytes(b"SELECT 1;\n")
            self.assertEqual(checksum(first), checksum(second))

    def test_modified_history_fails_closed(self):
        with tempfile.TemporaryDirectory(prefix="shopagent-catalog-") as directory:
            root = Path(directory)
            shutil.copytree(ROOT / "backend/db", root / "backend/db")
            (root / "backend/sql").mkdir()
            for path in (ROOT / "backend/sql").glob("[0-9][0-9][0-9]_*.sql"):
                shutil.copyfile(path, root / "backend/sql" / path.name)
            target = root / "backend/sql/005_add_product_logical_delete.sql"
            target.write_text(target.read_text(encoding="utf-8") + "\n-- changed\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "historical SQL"):
                validate(root)
