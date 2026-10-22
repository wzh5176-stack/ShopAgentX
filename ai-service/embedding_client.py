from functools import lru_cache
from pathlib import Path

from sentence_transformers import SentenceTransformer


MODEL_DIR = Path(__file__).resolve().parent / "model_cache" / "bge-small-zh-v1.5"


@lru_cache(maxsize=1)
def get_embedding_model() -> SentenceTransformer:
    return SentenceTransformer(
        str(MODEL_DIR),
        device="cpu",
        local_files_only=True
    )


def embed_texts(texts: list[str]):
    model = get_embedding_model()
    return model.encode(texts, normalize_embeddings=True)


if __name__ == "__main__":
    vectors = embed_texts([
        "演示耳机 K1 的充电盒使用 USB-C 接口。",
        "包装内不包含充电头。"
    ])

    print("向量形状：", vectors.shape)
    print("第一段的前5个数字：", vectors[0][:5])