import httpx


def main():
    try:
        with httpx.stream(
            "POST",
            "http://127.0.0.1:8000/api/model/chat/stream",
            json={"message": "请用两句话介绍AI导购。"},
            timeout=30.0
        ) as response:
            response.raise_for_status()
            print("响应类型：", response.headers.get("content-type"))
            print("下面显示服务器逐步发来的事件：", flush=True)

            for line in response.iter_lines():
                print(line, flush=True)
    except httpx.HTTPError as exc:
        print("请求失败，错误类型：", type(exc).__name__)
        print("请确认Python服务已启动，并检查接口地址。")


if __name__ == "__main__":
    main()