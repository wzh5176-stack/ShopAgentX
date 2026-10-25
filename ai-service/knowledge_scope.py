"""先确定商品资料范围，再检索；价格与库存仍以Java业务为准。"""
import re
from knowledge_loader import KnowledgeDocument


def normalized(text: str) -> str:
    return re.sub(r"[\s-]+", "", text).casefold()


def named_keys(question: str, documents: list[KnowledgeDocument]) -> set[str]:
    text = normalized(question)
    found = set()
    for document in documents:
        for alias in document.aliases:
            token = normalized(alias)
            if re.search(r"(?<![a-z0-9])" + re.escape(token) + r"(?![a-z0-9])", text):
                found.add(document.key)
                break
    return found


def resolve_scope(question: str, documents: list[KnowledgeDocument], product_title: str | None = None,
                  product_brand: str | None = None) -> tuple[str | None, str | None]:
    named = named_keys(question, documents)
    if product_title is not None or product_brand is not None:
        matches = [doc for doc in documents if doc.title == product_title and doc.brand == product_brand]
        if len(matches) != 1:
            return None, "这个商品暂时没有配套资料，请查看商品介绍或选择其他商品。"
        selected = matches[0].key
        if named - {selected}:
            return None, "问题中的型号与所选商品不一致，请切换商品或修改问题。"
        return selected, None
    if len(named) == 1:
        return next(iter(named)), None
    if len(named) > 1:
        return None, "本次资料问答请先选择一个商品；不同型号请分别查询后比较。"
    return None, "请先选择商品，或在问题中写明具体型号，例如 E02。"


def has_supported_topic(question: str, document: KnowledgeDocument) -> bool:
    text = normalized(question)
    return any(normalized(topic) in text for topic in document.topics)
