from pathlib import Path


KNOWLEDGE_DIR = Path(__file__).resolve().parent / "knowledge"


def load_knowledge() -> str:
    document_path = KNOWLEDGE_DIR / "demo_earphone_guide.md"
    return document_path.read_text(encoding="utf-8")


if __name__ == "__main__":
    print(load_knowledge())