from functools import lru_cache

import faiss

from embedding_client import embed_texts
from knowledge_loader import load_knowledge
from knowledge_splitter import split_knowledge


@lru_cache(maxsize=1)
def build_index():
    chunks = split_knowledge(load_knowledge())
    if not chunks:
        raise ValueError("知识资料中没有可检索的片段")

    texts = []
    for chunk in chunks:
        texts.append(chunk["section"] + "\n" + chunk["content"])

    vectors = embed_texts(texts)
    index = faiss.IndexFlatIP(vectors.shape[1])
    index.add(vectors)
    return index, chunks


def search_knowledge(question: str, top_k: int = 2) -> list[dict]:
    if not question.strip():
        raise ValueError("问题不能为空")
    if top_k < 1:
        raise ValueError("检索数量必须大于0")

    index, chunks = build_index()
    query = "为这个句子生成表示以用于检索相关文章：" + question.strip()
    query_vectors = embed_texts([query])
    scores, positions = index.search(query_vectors, min(top_k, len(chunks)))

    results = []
    for score, position in zip(scores[0], positions[0]):
        results.append({
            "score": float(score),
            "chunk": chunks[int(position)]
        })
    return results


if __name__ == "__main__":
    results = search_knowledge("K1的充电接口是什么？")
    for result in results:
        chunk = result["chunk"]
        print(chunk["source"], chunk["section"], result["score"])
        print(chunk["content"])
        print()