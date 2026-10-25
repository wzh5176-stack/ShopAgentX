from dataclasses import dataclass
import hashlib
import json
from pathlib import Path
import re


KNOWLEDGE_DIR = Path(__file__).resolve().parent / "knowledge"


@dataclass(frozen=True)
class KnowledgeDocument:
    key: str
    title: str
    brand: str
    aliases: tuple[str, ...]
    version: str
    source: str
    topics: tuple[str, ...]
    text: str


def load_documents(directory: Path = KNOWLEDGE_DIR) -> list[KnowledgeDocument]:
    directory = Path(directory).resolve()
    catalog = json.loads((directory / "catalog.json").read_text(encoding="utf-8"))
    documents, keys, identities = [], set(), set()
    for item in catalog["products"] + catalog.get("legacy_documents", []):
        key = item["key"]
        identity = (item["title"], item["brand"])
        if not re.fullmatch(r"demo-[a-z0-9-]{1,100}", key) or key in keys or identity in identities:
            raise ValueError("资料标识或商品身份重复")
        keys.add(key)
        identities.add(identity)
        source = item["source"]
        path = (directory / source).resolve()
        if not path.is_relative_to(directory) or path.suffix != ".md" or not path.is_file():
            raise ValueError("资料路径不合法或不存在")
        text = path.read_text(encoding="utf-8")
        if len(text) > 100000 or not text.strip() or not item["aliases"]:
            raise ValueError("资料内容或别名不合法")
        documents.append(KnowledgeDocument(key, item["title"], item["brand"], tuple(item["aliases"]),
                                           item["version"], source, tuple(item["topics"]), text))
    return documents


def document_fingerprint(documents: list[KnowledgeDocument]) -> str:
    content = [{"key": d.key, "title": d.title, "brand": d.brand, "aliases": d.aliases,
                "version": d.version, "source": d.source, "topics": d.topics, "text": d.text} for d in documents]
    return hashlib.sha256(json.dumps(content, ensure_ascii=False, sort_keys=True).encode()).hexdigest()


def load_knowledge() -> str:
    document_path = KNOWLEDGE_DIR / "demo_earphone_guide.md"
    return document_path.read_text(encoding="utf-8")


if __name__ == "__main__":
    print(load_knowledge())
