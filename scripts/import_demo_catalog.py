"""可重放的演示商品导入；不更新已有商品、库存或订单，不执行DDL。"""
import argparse
from collections import Counter
from decimal import Decimal
import json
import os
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "ai-service/knowledge/catalog.json"


def load_catalog(path=CATALOG):
    data = json.loads(Path(path).read_text(encoding="utf-8"))
    products = data["products"]
    seen, keys = set(), set()
    for item in products:
        identity = (item["title"], item["brand"])
        if identity in seen or item['key'] in keys or not re.fullmatch(r"demo-[a-z]+-[a-z][0-9]{2}", item["key"]):
            raise ValueError("duplicate or invalid demo identity")
        seen.add(identity)
        keys.add(item['key'])
        if not item["description"].startswith("虚构演示商品") or "演示" not in item["brand"]:
            raise ValueError("demo labeling required")
        if not re.fullmatch(r"[0-9]{1,8}\.[0-9]{2}", item["price"]) or Decimal(item["price"]) <= 0:
            raise ValueError("invalid demo price")
        if type(item["initialStock"]) is not int or not 0 <= item["initialStock"] <= 1000:
            raise ValueError("invalid initial stock")
        if not re.fullmatch(r"/shopagent/catalog/[a-z][0-9]{2}\.svg", item["imageUrl"]):
            raise ValueError("invalid demo illustration")
        if not (ROOT / "backend/src/main/resources/static" / item["imageUrl"].lstrip("/")).is_file():
            raise ValueError("illustration missing")
    return data


def connect(host, port, database, user, password):
    if database not in {"shopagent", "shopagent_regression"}:
        raise ValueError("only explicit ShopAgent databases are allowed")
    import pymysql
    return pymysql.connect(host=host, port=port, database=database, user=user, password=password,
                           charset="utf8mb4", autocommit=False, connect_timeout=5, read_timeout=30, write_timeout=30)


def import_catalog(connection, catalog, apply=False):
    inserted, existing = [], []
    with connection.cursor() as cursor:
        cursor.execute("SELECT VERSION(), DATABASE()")
        version, database = cursor.fetchone()
        if "MariaDB" in version or database not in {"shopagent", "shopagent_regression"}:
            raise ValueError("unexpected database engine or target")
        cursor.execute("SELECT GET_LOCK('shopagent_demo_catalog_v1', 15)")
        if cursor.fetchone()[0] != 1:
            raise RuntimeError("another catalog import is active")
        try:
            connection.begin()
            for item in catalog["products"]:
                cursor.execute("SELECT id FROM products WHERE title=%s AND brand=%s FOR UPDATE", (item["title"], item["brand"]))
                matches = cursor.fetchall()
                if len(matches) > 1:
                    raise ValueError("ambiguous existing demo product; no changes applied")
                if matches:
                    product_id = matches[0][0]
                    cursor.execute("SELECT product_id FROM product_inventory WHERE product_id=%s", (product_id,))
                    if not cursor.fetchone():
                        raise ValueError("existing product has no inventory row; manual review required")
                    existing.append(item["key"])
                    continue
                if apply:
                    cursor.execute("""INSERT INTO products(title,brand,price,image_url,description,status,deleted)
                                      VALUES(%s,%s,%s,%s,%s,1,0)""",
                                   (item["title"], item["brand"], Decimal(item["price"]), item["imageUrl"], item["description"]))
                    product_id = cursor.lastrowid
                    cursor.execute("INSERT INTO product_inventory(product_id,available_quantity,reserved_quantity) VALUES(%s,%s,0)",
                                   (product_id, item["initialStock"]))
                inserted.append(item["key"])
            if apply:
                connection.commit()
            else:
                connection.rollback()
        except BaseException:
            connection.rollback()
            raise
        finally:
            cursor.execute("SELECT RELEASE_LOCK('shopagent_demo_catalog_v1')")
    return {"database": database, "mysql_version": version, "applied": apply,
            "new_products" if apply else "would_insert": len(inserted), "preserved_existing": len(existing),
            "categories": dict(Counter(item["category"] for item in catalog["products"])), "catalog_version": catalog["version"]}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=3306)
    parser.add_argument("--database", default="shopagent")
    parser.add_argument("--user", default="shopagent_app")
    parser.add_argument("--password-env", default="SHOPAGENT_DB_PASSWORD")
    parser.add_argument("--local-config", action="store_true", help="consume this project's ignored local datasource config without printing it")
    parser.add_argument("--apply", action="store_true", help="default is read-only plan")
    args = parser.parse_args()
    catalog = load_catalog()
    password = os.environ.get(args.password_env)
    if args.local_config:
        import yaml
        settings = yaml.safe_load((ROOT / "backend/config/application-local.yml").read_text(encoding="utf-8"))
        password = settings["spring"]["datasource"]["password"]
    if not password:
        parser.error("database password environment is missing; do not put passwords in arguments")
    connection = connect(args.host, args.port, args.database, args.user, password)
    try:
        print(json.dumps(import_catalog(connection, catalog, args.apply), ensure_ascii=False, indent=2))
    finally:
        connection.close()


if __name__ == "__main__":
    main()
