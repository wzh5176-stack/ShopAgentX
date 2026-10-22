from model_client import ask_model


def main():
    message = "请只用一句简短的中文介绍你自己。"
    answer = ask_model(message)
    print("模型回答：")
    print(answer)


if __name__ == "__main__":
    main()