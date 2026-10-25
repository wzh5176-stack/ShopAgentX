from knowledge_loader import load_documents
from knowledge_scope import has_supported_topic, resolve_scope


def search_knowledge(question: str, top_k: int = 3, product_key: str | None = None) -> list[dict]:
    if not question.strip() or not 1 <= top_k <= 3:
        raise ValueError("问题或检索数量不符合要求")
    documents = load_documents()
    if product_key is None:
        product_key, error = resolve_scope(question, documents)
        if error:
            return []
    document = next((doc for doc in documents if doc.key == product_key), None)
    if document is None or not has_supported_topic(question, document):
        return []
    # 在型号范围内检索，不先全局取top_k再过滤，避免其他型号挤占候选。
    import faiss
    import numpy as np
    from embedding_client import embed_texts
    from knowledge_index import knowledge_snapshot
    _, _, chunks, vectors = knowledge_snapshot()
    positions = [index for index, chunk in enumerate(chunks) if chunk["product_key"] == product_key]
    selected = np.ascontiguousarray(vectors[positions], dtype="float32")
    index = faiss.IndexFlatIP(selected.shape[1])
    index.add(selected)
    query = embed_texts(["为这个句子生成表示以用于检索相关文章：" + question.strip()])
    scores, local_positions = index.search(np.asarray(query, dtype="float32"), min(top_k, len(positions)))
    return [{"score": float(score), "chunk": chunks[positions[int(position)]]}
            for score, position in zip(scores[0], local_positions[0]) if position >= 0]


if __name__ == "__main__":
    results = search_knowledge("E02支持无线充电吗？")
    for result in results:
        chunk = result["chunk"]
        print(chunk["source"], chunk["section"], result["score"])
        print(chunk["content"])
        print()
