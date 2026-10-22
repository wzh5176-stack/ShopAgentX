# 本地向量模型复制与只读挂载（用户手动）

2026-10-21用户反馈构建完成。助手SSH只读确认shopagent-ai:dev镜像ID75c4e34760f7已存在（显示占用2.07GB），并未代run导入或验证检索。Windows模型D:\ShopAgent-X\ai-service\model_cache\bge-small-zh-v1.5共11文件96,405,966字节，Linux指定工程范围未发现同名模型目录。VM当前1.9Gi内存、available1.4Gi、无swap、根盘剩22G，仅单次快照不等于整栈容量评估完成。

## 本组目标

让Python容器加载本地模型并执行knowledge_search.py，验证文件读取/模型编码/FAISS检索，不调用DeepSeek、不提供Key、不改正式源码或现有服务。模型bge-small-zh-v1.5把文本变为向量，与DeepSeek生成回答职责不同。

## 1. 用户在Linux SSH标签创建目录

```bash
mkdir -p /home/shopagent/shopagent/models
```

## 2. 用户在Windows PowerShell复制模型

```powershell
scp -r -F "D:\VMware work\ShopAgent-Ubuntu24\access\ssh_config" "D:\ShopAgent-X\ai-service\model_cache\bge-small-zh-v1.5" shopagent-linux:shopagent/models/
```

目标完整目录为/home/shopagent/shopagent/models/bge-small-zh-v1.5，不复制.venv或IDE配置。模型约96MB，保留1_Pooling子目录及所有文件。

## 3. 用户回Linux检查并开放模型读取权限

```bash
ls -lh /home/shopagent/shopagent/models/bge-small-zh-v1.5
chmod -R a+rX /home/shopagent/shopagent/models/bge-small-zh-v1.5
```

chmod只作用这份公开模型；-R含子目录，a+rX让所有用户可读、目录可进入（既有可执行文件保持可执行），不赋予额外写权限，使容器内普通用户10001可读取。路径必须是模型目录，不扩展到项目或配置目录。

## 4. 用户运行临时离线检索容器

```bash
sudo docker run --rm --network none --mount type=bind,src=/home/shopagent/shopagent/models/bge-small-zh-v1.5,dst=/app/model_cache/bge-small-zh-v1.5,readonly shopagent-ai:dev python knowledge_search.py
```

Dockerfile CMD被python knowledge_search.py替代；工作目录/app。bind将Linux宿主机目录显示为容器内代码固定查找路径；readonly拒绝容器写模型，文件仍在Linux外部目录。--network none禁网络，local_files_only代码只加载本地模型；未提供Key，脚本只检索，不生成回答。--rm结束删临时容器，不删镜像或外部模型。

knowledge_search.py中的现有main会查询“K1的充电接口是什么？”，输出候选来源/章节/相似度/正文，预期候选包含“充电方式”与USB-C。可能先打印库提示/加载进度；看实际候选与末尾是否异常。相似度不是准确率。不要求本地无模型生成用户可见最终答案。

## 本组反馈与下一组

用户反馈复制结果与检索终端输出；失败保留错误，不在线重新下载模型或关证书检查。助手未复制/chmod/run，检索结果待用户操作。

下一先评估整套部署资源（当前VM仅约2GB，不预设可同时稳定运行全部容器），再数据库备份与Linux独立演示库、外部私密配置和总Compose。Windows开发库保留。Java/Python镜像构建已只读确认，正常导入/真实HTTP/RAG、旧Nginx故障与代理流式的具体验收边界分别保留，不能只凭镜像存在当全链完成。

依据：https://docs.docker.com/engine/storage/bind-mounts/ 。
