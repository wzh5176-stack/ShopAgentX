"""只读校验 SQL 目录、历史指纹和空库基线。绝不连接数据库或执行 SQL。"""
import hashlib
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]


def split_sql(text):
    """处理本仓库的普通 SQL、引号和注释；不支持存储过程/DELIMITER。"""
    if re.search(r"(?im)^\s*DELIMITER\b", text):
        raise ValueError("DELIMITER requires a reviewed migration tool")
    statements, part = [], []
    quote = None
    index = 0
    while index < len(text):
        char = text[index]
        if quote:
            part.append(char)
            if char == "\\" and index + 1 < len(text):
                index += 1
                part.append(text[index])
            elif char == quote:
                if index + 1 < len(text) and text[index + 1] == quote:
                    index += 1
                    part.append(text[index])
                else:
                    quote = None
        elif char in "'\"`":
            quote = char
            part.append(char)
        elif text.startswith("--", index) and (index + 2 == len(text) or text[index + 2].isspace()):
            end = text.find("\n", index)
            index = len(text) if end < 0 else end
            part.append("\n")
        elif text.startswith("/*", index):
            end = text.find("*/", index + 2)
            if end < 0:
                raise ValueError("unterminated SQL comment")
            index = end + 1
            part.append(" ")
        elif char == ";":
            if "".join(part).strip():
                statements.append("".join(part).strip())
            part = []
        else:
            part.append(char)
        index += 1
    if quote:
        raise ValueError("unterminated SQL quote")
    if "".join(part).strip():
        raise ValueError("SQL statement must end with semicolon")
    return statements


def checksum(path):
    # Windows CRLF 和 Linux LF 使用相同内容指纹；不忽略实际 SQL 或注释改动。
    text = path.read_text(encoding="utf-8-sig").replace("\r\n", "\n")
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def schema_statements(sql_dir, entries):
    statements = []
    for entry in entries:
        if entry["kind"] != "schema":
            continue
        for statement in split_sql((sql_dir / entry["file"]).read_text(encoding="utf-8-sig")):
            if re.match(r"(?is)^(CREATE|ALTER) TABLE\b", statement):
                statements.append(re.sub(r"(?i)\bshopagent\.", "", statement))
    return statements


def validate(root=ROOT):
    sql_dir = root / "backend/sql"
    db_dir = root / "backend/db"
    manifest = json.loads((db_dir / "migration-manifest.json").read_text(encoding="utf-8"))
    entries = manifest["history"]
    expected = [entry["file"] for entry in entries]
    actual = sorted(path.name for path in sql_dir.glob("[0-9][0-9][0-9]_*.sql"))
    if actual != expected or len(set(expected)) != len(expected):
        raise ValueError("SQL files differ from the reviewed migration manifest")
    if [int(name[:3]) for name in expected] != list(range(1, len(expected) + 1)):
        raise ValueError("migration numbering is not contiguous")
    for entry in entries:
        if entry["kind"] not in {"schema", "seed", "verify", "account", "grant"}:
            raise ValueError("unknown SQL category")
        if checksum(sql_dir / entry["file"]) != entry["sha256_lf"]:
            raise ValueError(f"historical SQL was modified: {entry['file']}")
    baseline = db_dir / "schema-baseline.sql"
    if checksum(baseline) != manifest["baseline_sha256_lf"]:
        raise ValueError("baseline checksum differs from manifest")
    statements = split_sql(baseline.read_text(encoding="utf-8"))
    if statements != schema_statements(sql_dir, entries):
        raise ValueError("baseline is not the reviewed schema-only history")
    tables = sorted(re.findall(r"(?im)^CREATE TABLE\s+([a-z_]+)\b", "\n".join(statements)))
    if tables != sorted(manifest["expected_tables"]):
        raise ValueError("baseline table inventory is wrong")
    return {"history_files": len(entries), "baseline_tables": len(tables),
            "baseline_statements": len(statements), "database_connected": False}


if __name__ == "__main__":
    print(json.dumps(validate(), ensure_ascii=False, indent=2))
