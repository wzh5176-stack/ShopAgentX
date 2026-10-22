import json
import ssl
from collections.abc import Iterator

import httpx

from model_config import MODEL_BASE_URL, MODEL_NAME, get_api_key


def ask_model_stream(message: str) -> Iterator[str]:
    payload = {
        "model": MODEL_NAME,
        "messages": [{"role": "user", "content": message}],
        "thinking": {"type": "disabled"},
        "stream": True,
        "max_tokens": 256
    }

    parts = []
    total_length = 0
    finished = False
    received_done = False

    with httpx.stream(
        "POST",
        f"{MODEL_BASE_URL}/chat/completions",
        headers={"Authorization": f"Bearer {get_api_key()}"},
        json=payload,
        verify=ssl.create_default_context(),
        timeout=20.0
    ) as response:
        response.raise_for_status()

        for line in response.iter_lines():
            if not line.startswith("data:"):
                continue

            data = line[5:].strip()
            if not data:
                continue
            if data == "[DONE]":
                received_done = True
                break

            chunk = json.loads(data)
            if not isinstance(chunk, dict) or not isinstance(chunk.get("choices"), list):
                raise ValueError("模型流式响应格式不正确")
            choices = chunk["choices"]
            if not choices:
                continue

            choice = choices[0]
            if not isinstance(choice, dict) or not isinstance(choice.get("delta"), dict):
                raise ValueError("模型流式片段格式不正确")
            content = choice.get("delta", {}).get("content")
            if content is not None:
                if not isinstance(content, str):
                    raise ValueError("模型文字片段格式不正确")
                if content:
                    if finished:
                        raise ValueError("模型结束后仍返回文字")
                    total_length += len(content)
                    if total_length > 2000:
                        raise ValueError("模型回答超过长度限制")
                    parts.append(content)
                    yield content

            finish_reason = choice.get("finish_reason")
            if finish_reason is not None:
                if finish_reason != "stop":
                    raise ValueError("模型回答未正常完成")
                finished = True

    if not received_done or not finished or not "".join(parts).strip():
        raise ValueError("模型没有返回完整有效的回答")