"""内容版本驱动的本地向量快照；不使用pickle，不加载远程模型。"""
import json
import os
from pathlib import Path
import threading
import hashlib
from zipfile import BadZipFile

from knowledge_loader import document_fingerprint, load_documents
from knowledge_splitter import split_knowledge

_LOCK = threading.RLock()
_SNAPSHOT = None
CACHE_DIR = Path(__file__).resolve().parent / "vector_store"


def knowledge_snapshot():
    global _SNAPSHOT
    documents = load_documents()
    # 更换模型、维度、归一化或分块约定时必须升级此版本，避免复用旧空间。
    signature = hashlib.sha256(('bge-small-zh-v1.5:512:normalized:chunks-v1:'
                               + document_fingerprint(documents)).encode()).hexdigest()
    with _LOCK:
        if _SNAPSHOT is not None and _SNAPSHOT[0] == signature:
            return _SNAPSHOT
        import numpy as np
        from embedding_client import embed_texts
        chunks = []
        for document in documents:
            chunks.extend(split_knowledge(document.text, key=document.key, source=document.source,
                                          title=document.title, version=document.version))
        if not chunks:
            raise ValueError("知识资料中没有可检索片段")
        cache = CACHE_DIR / f"knowledge-{signature}.npz"
        vectors = None
        if cache.is_file():
            try:
                with np.load(cache, allow_pickle=False) as saved:
                    if str(saved["signature"].item()) == signature and json.loads(str(saved["chunks"].item())) == chunks:
                        candidate = saved["vectors"]
                        if (candidate.ndim == 2 and candidate.shape[0] == len(chunks)
                                and candidate.shape[1] == 512 and np.isfinite(candidate).all()
                                and np.allclose(np.linalg.norm(candidate, axis=1), 1, atol=0.01)):
                            vectors = np.asarray(candidate, dtype="float32")
            except (OSError, ValueError, KeyError, EOFError, BadZipFile):
                pass
        if vectors is None:
            vectors = np.asarray(embed_texts([c["document"] + "\n" + c["section"] + "\n" + c["content"] for c in chunks]), dtype="float32")
            if (vectors.shape != (len(chunks), 512) or not np.isfinite(vectors).all()
                    or not np.allclose(np.linalg.norm(vectors, axis=1), 1, atol=0.01)):
                raise ValueError("资料向量格式错误")
            try:
                CACHE_DIR.mkdir(parents=True, exist_ok=True)
                temporary = cache.with_suffix(f".{os.getpid()}.tmp")
                with temporary.open("wb") as output:
                    np.savez_compressed(output, vectors=vectors, signature=signature,
                                        chunks=json.dumps(chunks, ensure_ascii=False))
                temporary.replace(cache)
            except OSError:
                pass
        _SNAPSHOT = signature, documents, chunks, vectors
        return _SNAPSHOT
