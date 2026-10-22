import httpx
import ssl

from model_config import MODEL_BASE_URL, MODEL_NAME, get_api_key


def ask_model(message: str,json_mode: bool = False) -> str:
    api_key = get_api_key()

    payload = {
        "model": MODEL_NAME,
        "messages": [
            {"role": "user", "content": message}
        ],
        "thinking": {"type": "disabled"},
        "stream": False,
        "max_tokens": 256
    }

    if json_mode:
        payload["response_format"] = {"type": "json_object"}

    response = httpx.post(
        f"{MODEL_BASE_URL}/chat/completions",
        headers={"Authorization": f"Bearer {api_key}"},
        json=payload,
        verify=ssl.create_default_context(),
        timeout=20.0
    )
    response.raise_for_status()

    result = response.json()
    choice = result["choices"][0]

    if choice["finish_reason"] != "stop":
        raise ValueError("模型回答未正常完成")

    answer = choice["message"]["content"]
    if not isinstance(answer, str) or not answer.strip():
        raise ValueError("模型没有返回有效回答")

    return answer.strip()