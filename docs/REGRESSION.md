# 自动化回归与工程检查

本轮把可重复执行的回归放进仓库标准目录。历史验收报告继续作为历史证据，不把它们的检查数量累加到新测试数量。

最近一次本机集中验收：2026-10-25库存闭环后，Java63 + MySQL业务12 + Python36 + 工程8 + 目录导入MySQL2，共121项通过。原91/114项是此前快照；最新范围见[库存闭环](INVENTORY_CLOSURE.md)。远端结果以对应提交的GitHub Actions为准。

## 默认回归覆盖

| 位置 | 执行内容 |
| --- | --- |
| `backend/src/test/java/.../service` | 下单参数/指纹、幂等竞争处理、服务端核价、本人订单条件、沙箱结算分支、通知签名、AI 返回与流式保存边界 |
| `backend/src/test/java/.../config` | 直接调用生产 Security 配置方法，验证角色、CSRF、精确通知放行与未知路径拒绝 |
| `backend/src/test/java/.../client` | SSE 中文、结束事件、截断、非法事件、数据长度和 UTF-8 |
| `ai-service/tests` | 预算、推荐候选、购物车意图、知识引用、流式错误/取消，以及真实 FastAPI 进程内 HTTP 协议 |
| `scripts/tests` | SQL 清单、历史改动拦截、跨平台校验和与 SQL 分句规则 |

默认测试不启动正式 Spring Boot 应用，不加载本机管理员配置或支付宝私密文件。模型用受控结果；通知测试每次生成临时 RSA 测试材料，只验本地签名。Python 真实 HTTP transport 被拦截；FastAPI TestClient 在进程内运行。

Python 的检索边界被测试替身替换，真实 `knowledge_service` 仍执行引用校验。本套不验证 BGE/FAISS 的召回质量，也不下载向量模型。`requirements-api.txt` 是从原完整依赖拆出的轻量 API 依赖；正式运行仍安装 `requirements.txt`，它同时引入 API 和 embedding 清单。

## 本机默认执行

在仓库根目录，Python、Maven 和 Java 17 已配置 PATH 时：

```text
python scripts/regression.py
```

当前开发机可直接使用以下 PowerShell 命令，不需要执行 `.ps1` 或修改执行策略：

```powershell
& "D:\ShopAgent-X\ai-service\.venv\Scripts\python.exe" "D:\ShopAgent-X\scripts\regression.py" --maven "D:\Maven\apache-maven-3.9.4-bin\apache-maven-3.9.4\bin\mvn.cmd" --java-home "C:\Users\wzh51\.jdks\ms-17.0.20"
```

脚本实际运行 SQL 检查、工程测试、Python 测试和 Maven `clean verify`。任何一组失败，最终退出码为非零。日志与 `summary.json` 保存在 `tmp/regression/<UTC时间>/`，Java 标准 XML 在 `backend/target/surefire-reports/`。

Java 单独运行：`mvn -B -ntp verify`（进入 backend）。Python 单独运行：`python -m unittest discover -s tests -v`（进入 ai-service）。

首次克隆需要安装API与导入测试依赖：`python -m pip install -r ai-service/requirements-api.txt -r scripts/requirements.txt`。测试无需额外pytest，完整向量评测另需RAG环境。

## MySQL 集成回归

`mvn -Pmysql-it verify` 另外执行 `MySqlTradeIT`，使用真实 MySQL、真实 Mapper 和业务方法，在 Spring `TransactionTemplate` 中运行。它验证：

- 空库 12 表及 CHECK 约束。
- 20 个并发请求竞争 5 件库存，只有 5 次预占成功。
- 第二商品预占失败时，第一商品预占及订单写入回滚。
- 同用户同请求编号被数据库唯一约束拦截，不二次预占。
- 两个并发成功结果只消耗一次库存。
- 第二商品结算失败时，第一商品扣减与订单/支付状态回滚。
- 重复超时处理只释放一次库存。

支付结果为合成数据；这些通过不代表官方交易关单、真实通知隧道或新一次沙箱付款通过。

统一入口在Java集成组后另执行`catalog_mysql_it.py`：导入预览无写入、36款新增、重复导入不覆盖已消耗/预占库存和修改后价格状态，以及后段校验失败回滚前段插入。只允许同一个专用隔离回归库，不接触正式库。

### 当前 Windows 的全套隔离运行

```powershell
& "D:\ShopAgent-X\ai-service\.venv\Scripts\python.exe" "D:\ShopAgent-X\scripts\local_mysql_regression.py" --mysql-bin "D:\MySQL\mysql-8.0.34-winx64\mysql-8.0.34-winx64\bin" --maven "D:\Maven\apache-maven-3.9.4-bin\apache-maven-3.9.4\bin\mvn.cmd" --java-home "C:\Users\wzh51\.jdks\ms-17.0.20"
```

这个入口用现有 MySQL 二进制新建临时数据目录、随机 loopback 端口和随机测试账号密码；运行同一套回归后用 mysqladmin 正常关闭临时实例，再清理自己创建的临时目录。不会启动或关闭原来的 MySQL，不需要 VMware/Docker Desktop。启动失败的日志放在 `tmp/regression-mysql/`；未正常关闭时保留临时目录供诊断。

### CI 或自己准备空库

必须显式设置以下环境变量，再运行 `python scripts/regression.py --mysql`：

| 变量 | 含义 |
| --- | --- |
| `SHOPAGENT_TEST_MYSQL_ALLOW` | 固定为 `empty-regression-db` |
| `SHOPAGENT_TEST_MYSQL_URL` | `jdbc:mysql://127.0.0.1:<端口>/shopagent_regression?...` |
| `SHOPAGENT_TEST_MYSQL_USER` | 仅测试实例的账号 |
| `SHOPAGENT_TEST_MYSQL_PASSWORD` | 仅测试实例的密码 |

必须是新建空库。每次重跑换一个干净的临时实例，不要向现有业务库运行，也不要为重跑测试删除真实库。

## GitHub 自动执行

`.github/workflows/regression.yml` 配置 main/feature 分支推送、PR 和手动触发。GitHub Runner 使用 Java 17、Python 3.12 和一次性的 MySQL 8.4.11 服务，安装轻量 API 依赖后执行全套回归，保存报告 14 天。

CI 不配置 DeepSeek 或支付宝密钥，不建立公网隧道，不部署，不自动合并。工作流文件需提交并推送后才会在 GitHub 运行；本机通过不能写成 GitHub Actions 已通过。

## 本轮边界与后续

这是一套关键链路回归，不是整个商城所有接口的端到端覆盖。MySQL 集成组直接验证内部业务方法和数据库事务，生产事务代理、跨服务网络、Redis 会话、真实浏览器和外部网关需要各自集成验收。

本地 MySQL 8.0.34 与 CI 配置 8.4.11 需分开记录结果。模型输出替身用于稳定验证程序边界，不能据此宣称真实模型准确率。既有真实沙箱 WAIT_BUYER_PAY 主动关单待验分支没有在本轮补验。

数据库迁移说明见 [结构与迁移约定](../backend/db/README.md)。
