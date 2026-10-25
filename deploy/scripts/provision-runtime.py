"""Provision this project's Linux deployment without logging credentials.

Run on the guest as the SSH owner. Database authentication consumes the existing
root secret inside its container; secrets are never command-line SQL output.
"""
import hashlib
import json
import os
from pathlib import Path
import secrets
import subprocess

BASE = Path(__file__).resolve().parents[2]
PRIVATE = BASE / "secrets"
PRIVATE.mkdir(mode=0o700, exist_ok=True)
PRIVATE.chmod(0o700)
os.umask(0o077)
COMPOSE = ["sudo", "-n", "docker", "compose", "-f", str(BASE / "deploy/compose.mysql.yml")]


def sql(statement):
    command = COMPOSE + ["exec", "-T", "mysql", "sh", "-c",
        'export MYSQL_PWD="$(cat /run/secrets/mysql_root_password)"; '
        'exec mysql --user=root --batch --skip-column-names']
    result = subprocess.run(command, input=statement, text=True, capture_output=True)
    if result.returncode:
        raise RuntimeError("Database provisioning failed; no credentials were printed")
    return result.stdout.strip()


tables = ("products", "app_users", "chat_conversations", "chat_messages", "cart_items", "cart_actions")
baseline = {table: int(sql(f"SELECT COUNT(*) FROM shopagent.{table};")) for table in tables}
exists = sql("SELECT COUNT(*) FROM mysql.user WHERE user='shopagent_app' AND host='%';") == "1"
password_file = PRIVATE / "spring.datasource.password"
if exists and not password_file.is_file():
    raise RuntimeError("Existing application account has no saved secret; it was not changed")
if not exists:
    password = password_file.read_text() if password_file.is_file() else secrets.token_urlsafe(32)
    literal = password.replace("\\", "\\\\").replace("'", "\\'")
    sql("SET SESSION sql_mode=REPLACE(@@sql_mode,'NO_BACKSLASH_ESCAPES',''); "
        f"CREATE USER 'shopagent_app'@'%' IDENTIFIED BY '{literal}';")
    password_file.write_text(password)

permissions = {
    "products": "SELECT, INSERT, UPDATE",
    "app_users": "SELECT, INSERT",
    "chat_conversations": "SELECT, INSERT, UPDATE",
    "chat_messages": "SELECT, INSERT",
    "cart_items": "SELECT, INSERT, UPDATE, DELETE",
    "cart_actions": "SELECT, INSERT, UPDATE",
}
for table, privileges in permissions.items():
    sql(f"GRANT {privileges} ON shopagent.{table} TO 'shopagent_app'@'%';")
password_file.chmod(0o444)

admin_file = PRIVATE / "admin-local.properties"
admin_credentials = PRIVATE / "runtime-admin-credentials.json"
if not admin_file.exists():
    password = secrets.token_urlsafe(24)
    salt = secrets.token_bytes(16)
    encoded = (salt + hashlib.pbkdf2_hmac("sha256", password.encode(), salt, 310000, 32)).hex()
    admin_file.write_text("shopagent.admin.password-hash={pbkdf2@SpringSecurity_v5_8}" + encoded + "\n")
    admin_credentials.write_text(json.dumps({"username": "shopagent_admin", "password": password}))
    admin_credentials.chmod(0o600)
admin_file.chmod(0o444)

key_file = PRIVATE / "deepseek-api-key.txt"
if not key_file.exists():
    key_file.touch(mode=0o600)
key_file.chmod(0o444)
after = {table: int(sql(f"SELECT COUNT(*) FROM shopagent.{table};")) for table in tables}
if after != baseline:
    raise RuntimeError("Business table counts changed during credential provisioning")
report = {"business_counts_unchanged": True, "table_counts": after,
          "application_account": "shopagent_app@%", "table_grants": permissions,
          "admin_credentials_created": admin_credentials.is_file(),
          "deepseek_key_configured": key_file.stat().st_size > 0}
print(json.dumps(report))
