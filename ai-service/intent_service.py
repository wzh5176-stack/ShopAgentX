from decimal import Decimal, InvalidOperation

from intent_schema import SearchIntent
from model_client import ask_model


def normalize_intent(intent: SearchIntent) -> SearchIntent:
    intent.keyword = intent.keyword.strip()

    if intent.maxPrice is not None:
        try:
            price = Decimal(intent.maxPrice)
        except InvalidOperation as exc:
            raise ValueError("最高预算不是有效金额") from exc

        if (not price.is_finite()
                or price < 0
                or price > Decimal("99999999.99")):
            raise ValueError("最高预算超出有效范围")

        rounded = price.quantize(Decimal("0.01"))
        if price != rounded:
            raise ValueError("最高预算最多保留两位小数")

        intent.maxPrice = format(rounded, ".2f")

    return intent


def extract_intent(question: str) -> SearchIntent:
    prompt = (
        "从用户需求中提取商品搜索条件，只返回JSON对象，不要解释或Markdown。\n"
        '格式示例：{"keyword":"跑鞋","maxPrice":"500.00"}。\n'
        "keyword取商品品类或品牌，最多50个字符；无法识别时用空字符串。\n"
        "maxPrice只提取明确的单件最高预算，单位元，用金额字符串；\n"
        "没有明确最高预算时用null，不要把最低预算当最高预算。\n"
        f"用户需求：{question}"
    )

    answer = ask_model(prompt, json_mode=True)
    intent = SearchIntent.model_validate_json(answer)
    return normalize_intent(intent)