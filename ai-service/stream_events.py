import json
import logging
from collections.abc import Iterator

import httpx

from model_stream_client import ask_model_stream

logger = logging.getLogger(__name__)


def encode_event(event: str, data: dict) -> str:
    body = json.dumps(data, ensure_ascii=False)
    return f"event: {event}\ndata: {body}\n\n"


def model_stream_events(message: str) -> Iterator[str]:
    parts = []
    stream = ask_model_stream(message)

    try:
        for piece in stream:
            parts.append(piece)
            yield encode_event("delta", {"content": piece})

        yield encode_event("done", {
            "answer": "".join(parts),
            "service": "shopagent-ai"
        })
    except (httpx.HTTPError, ValueError, KeyError, IndexError, TypeError):
        logger.exception("模型流式调用失败")
        yield encode_event("error", {
            "code": "AI_SERVICE_UNAVAILABLE",
            "message": "模型服务暂时不可用，请稍后重试"
        })
    finally:
        stream.close()