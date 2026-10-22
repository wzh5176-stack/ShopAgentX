# Python 镜像：选择性复制、Linux构建、离线导入检查

2026-10-21：助手只读确认ai-service/Dockerfile与.dockerignore已保存且符合上一课；未代操作Linux或构建。用户仍手动执行以下命令；Java镜像构建、代理故障/SSE结果尚未明确反馈。

## 1. 已登录Linux的SSH标签

```bash
mkdir -p /home/shopagent/shopagent/build/ai-service
free -h
df -h /
```

free显示内存，df显示磁盘，保留结果供后续完整部署资源评估；本组pip安装CPUtorch/faiss等比Java镜像耗时/空间更多。若未登录，在新Windows标签先ssh -F "D:\VMware work\ShopAgent-Ubuntu24\access\ssh_config" shopagent-linux。保留原四通道转发，不在其阻塞窗口输入命令。

## 2. 新Windows PowerShell标签复制

四个构建文件：

```powershell
scp -F "D:\VMware work\ShopAgent-Ubuntu24\access\ssh_config" "D:\ShopAgent-X\ai-service\Dockerfile" "D:\ShopAgent-X\ai-service\.dockerignore" "D:\ShopAgent-X\ai-service\requirements.txt" "D:\ShopAgent-X\ai-service\requirements-embedding.txt" shopagent-linux:shopagent/build/ai-service/
```

根目录Python源码，使用PowerShell枚举并通过数组展开传给scp，避免依赖原生命令对Windows通配符的支持：

```powershell
$pythonSourceFiles = Get-ChildItem -LiteralPath "D:\ShopAgent-X\ai-service" -Filter "*.py" -File | Select-Object -ExpandProperty FullName
scp -F "D:\VMware work\ShopAgent-Ubuntu24\access\ssh_config" @pythonSourceFiles shopagent-linux:shopagent/build/ai-service/
```

知识资料：

```powershell
scp -r -F "D:\VMware work\ShopAgent-Ubuntu24\access\ssh_config" "D:\ShopAgent-X\ai-service\knowledge" shopagent-linux:shopagent/build/ai-service/
```

Get-ChildItem只枚举根py，Select-Object取完整路径；@pythonSourceFiles把数组各元素作为单独参数传入scp。scp -r递归复制knowledge目录。本组不复制整个ai-service/.venv/.idea/.env/model_cache；dockerignore不是scp过滤器，因此明确选择来源。

## 3. Linux构建

```bash
cd /home/shopagent/shopagent/build/ai-service
ls -la
ls knowledge
sudo docker build --pull --progress=plain -t shopagent-ai:dev .
```

预期Dockerfile/dockerignore/两个requirements/各py/knowledge齐备。--progress=plain显示每步日志；pip会根据两份清单安装Linux版依赖，可能下载较大CPUtorch。Windows环境能用不等于Linux依赖已验；如版本无包、网络或证书错误，停在具体错误定位，不自动去掉固定版本/关TLS/换源/清卷。

```bash
sudo docker image ls shopagent-ai:dev
```

镜像存在后继续下一条，不因失败仍执行docker run而意外拉同名远端镜像。

## 4. 临时容器导入检查

```bash
sudo docker run --rm --network none shopagent-ai:dev python -c "from main import app; print(app.title)"
```

预期ShopAgent AI Service。docker run从本地镜像启动临时容器，后面的python -c覆盖Dockerfile CMD；--rm运行结束删该临时容器，不删除镜像/其他容器；--network none隔离网络。只导入main并打印FastAPI标题，不启动Uvicorn/不发布8000/不使用APIKey/不发模型请求。模型加载在缓存函数内延迟执行，此步不证明模型文件已挂载、RAG可用或HTTP全链正常。

## 反馈与下一组

用户集中反馈free/df、镜像列表和服务标题；失败给出错阶段/末尾提示。助手未复制/构建/运行。下一先评估虚拟机资源，复制本地模型到Linux单独目录并只读挂载验证检索，再准备独立Linux数据库/外部配置与总Compose，保留Windows开发库。不要在环境尚缺时直接用当前镜像启动整套服务并宣称部署完成。

依据：https://docs.docker.com/reference/cli/docker/buildx/build/ ，https://docs.docker.com/reference/cli/docker/container/run/ 。
