from knowledge_loader import load_knowledge


def split_knowledge(text: str, *, key: str = "demo_earphone_guide", source: str = "demo_earphone_guide.md",
                    title: str | None = None, version: str | None = None) -> list[dict[str, str]]:
    parts = text.split("\n## ")
    document = title or parts[0].strip()
    if version:
        document += " · 资料版本 " + version + " · 虚构演示"
    chunks = []

    for number, part in enumerate(parts[1:], start=1):
        section, _, content = part.partition("\n")
        content = content.strip()

        if not content:
            continue

        if len(content) > 5000:
            raise ValueError("资料小节过长，请先拆分")
        chunks.append({
            "id": f"{key}:{number}",
            "source": source,
            "product_key": key,
            "document": document,
            "section": section.strip(),
            "content": content
        })

    return chunks


if __name__ == "__main__":
    text = load_knowledge()
    chunks = split_knowledge(text)

    for chunk in chunks:
        print(chunk["id"], chunk["section"])
        print(chunk["content"])
        print()
