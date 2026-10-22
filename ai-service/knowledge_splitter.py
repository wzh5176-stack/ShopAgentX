from knowledge_loader import load_knowledge


def split_knowledge(text: str) -> list[dict[str, str]]:
    parts = text.split("\n## ")
    document = parts[0].strip()
    chunks = []

    for number, part in enumerate(parts[1:], start=1):
        section, _, content = part.partition("\n")
        content = content.strip()

        if not content:
            continue

        chunks.append({
            "id": f"demo_earphone_guide:{number}",
            "source": "demo_earphone_guide.md",
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