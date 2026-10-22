# DeepSeek容器密钥与单次真实调用（用户手动）

2026-10-21前置只读：Windows容器YAML符合上课，Linux目标路径存在；Linux管理员配置444、数据库秘密444、父目录此前700，无deepseek-api-key.txt。不读取私密值，不代运行服务或模型。当前用户手动带教偏好有效，自动部署草稿不执行provision脚本。

## 1. 用户Linux隐藏输入密钥并保存

```bash
umask 077
read -r -s -p "粘贴 DeepSeek API Key：" shopagentDeepSeekKey
```

单独执行read后只粘Key值，不带DEEPSEEK_API_KEY=、Bearer、引号，按回车，再执行：

```bash
printf '\n'
if [ -n "$shopagentDeepSeekKey" ]; then
    printf '%s' "$shopagentDeepSeekKey" > /home/shopagent/shopagent/secrets/deepseek-api-key.txt
    chmod 444 /home/shopagent/shopagent/secrets/deepseek-api-key.txt
else
    printf '密钥为空，请重新输入，先不要继续。\n'
fi
unset shopagentDeepSeekKey
```

仅新建当前不存在文件；不要求发聊天或截图。父secrets700保护，特定文件444供普通容器用户读取，密钥不放公开YAML或镜像。

## 2. 用户修改 D:\ShopAgent-X\deploy\scripts\start-ai.sh

这是此前准备过的公开草稿，用户检查并替换完整内容（LF换行）：

```sh
#!/bin/sh
set -eu

key_file="${DEEPSEEK_API_KEY_FILE:-/run/secrets/deepseek_api_key}"
if [ ! -s "$key_file" ]; then
    printf '缺少 DeepSeek 密钥文件，无法启动模型服务。\n' >&2
    exit 1
fi

DEEPSEEK_API_KEY="$(cat "$key_file")"
export DEEPSEEK_API_KEY

exec "$@"
```

set-e失败停，-u未定义变量报错；:-给默认文件路径；-s要求文件存在且非空；读取后export提供给现有model_config.py的os.getenv；exec执行调用方传入的命令，正式为Uvicorn、本课为python model_probe.py。没有把密钥写进脚本。缺失密钥时明确失败，不用健康UP掩盖实际不能调模型。

## 3. 用户复制公开脚本

Linux：

```bash
mkdir -p /home/shopagent/shopagent/deploy/scripts
```

Windows PowerShell：

```powershell
scp -F "D:\VMware work\ShopAgent-Ubuntu24\access\ssh_config" "D:\ShopAgent-X\deploy\scripts\start-ai.sh" shopagent-linux:shopagent/deploy/scripts/start-ai.sh
```

回Linux：

```bash
chmod 644 /home/shopagent/shopagent/deploy/scripts/start-ai.sh
```

脚本被/bin/sh读取，不依赖可执行位；Windows编辑器选择LF，避免CRLF造成set/参数异常。未复制任何WindowsIDE私密配置。

## 4. 用户临时容器调用一次真实DeepSeek

仅Linux执行整块，反斜杠为续行：

```bash
sudo docker run --rm --network shopagent-runtime_default \
  --mount type=bind,src=/home/shopagent/shopagent/secrets/deepseek-api-key.txt,dst=/run/secrets/deepseek_api_key,readonly \
  --mount type=bind,src=/home/shopagent/shopagent/deploy/scripts/start-ai.sh,dst=/opt/shopagent/start-ai.sh,readonly \
  --entrypoint /bin/sh \
  shopagent-ai:dev /opt/shopagent/start-ai.sh python model_probe.py
```

复用已构建镜像和旧model_probe.py的单句自我介绍；这次允许网络、真实发一次模型请求，Key和脚本只读挂载，脚本给Python提供环境变量。未挂向量模型因为本测试不做RAG。预期模型回答且无异常。401/余额/网络/证书/模型名错误均需要具体定位，不关TLS或改模型掩盖。用户反馈回答/状态，不发Key或Authorization内容。

## 下一步与边界

助手未代写正式脚本或密钥、未复制/发模型请求。测试成功只说明单次容器模型连接，不是多轮/流式/RAGHTTP全链已验。下一用户手动完整替换compose.runtime.yml：移除草稿重复DB/Redis/AI配置env，挂application-container.yml并启用containerprofile，保留MySQL现有项目/卷、秘密和原管理员配置，再复制Nginx/runtime脚本并启动整套环境。provision-runtime.py禁止运行以免覆盖用户已配秘密/管理员。最终依赖/登录/商品/流式/会话/故障回归后再称部署完成。
