import os


MODEL_BASE_URL = "https://api.deepseek.com"
MODEL_NAME = "deepseek-flash"


def get_api_key() -> str:
    api_key = os.getenv("DEEPSEEK_API_KEY", "").strip()

    if not api_key:
        raise ValueError("请先在运行环境中配置 DEEPSEEK_API_KEY")

    return api_key