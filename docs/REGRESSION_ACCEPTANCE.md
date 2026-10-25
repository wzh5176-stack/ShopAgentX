最新库存闭环已通过121项回归；本文保留此前91项工程整理的历史证据，最新范围见[库存闭环](INVENTORY_CLOSURE.md)。

# 自动化回归本机验收 · 2026-10-25

用户授权直接完成自动化回归与工程整理。本轮保留业务逻辑和已有数据，不部署、不连接正式业务库、不调用真实模型或官方支付、不提交推送。

## 最终结果

执行仓库内 `scripts/local_mysql_regression.py`，由它创建临时 MySQL 实例，再执行 `scripts/regression.py --mysql`。

| 组别 | 用例数 | 结果 |
| --- | ---: | --- |
| Java 业务、权限、通知验签和 AI 协议 | 54 | 全通过，0 跳过 |
| 真实 MySQL 事务、约束和并发 | 7 | 全通过，0 跳过 |
| Python 业务及 FastAPI 进程内 HTTP | 25 | 全通过 |
| SQL 工程校验测试 | 5 | 全通过 |

合计 91 个自动化用例。另有 SQL 清单检查：27 个历史文件、12 表空库基线、16 条结构语句一致。Maven `clean verify` 完成测试和打包。此数量只属于本轮，不与历史验收数字叠加。

最终机器报告：`tmp/regression/20261025T004738053266Z/summary.json`，`passed=true`、各组退出码 0。日志在同目录，标准 Java XML 在 `backend/target/surefire-reports/` 和 `failsafe-reports/`。这些运行输出不入 Git。

另新建干净 Python 3.12 虚拟环境，只从公开 PyPI 安装 `requirements-api.txt`，25 个 Python 用例全部通过，未安装 torch、FAISS 或本地 BGE 模型。Python 语法、工作流 YAML/关键字段、改动文档链接及 `git diff --check` 通过。

## 运行隔离和清理

- 本机真实数据库测试使用 MySQL 8.0.34，临时目录、随机 loopback 端口、独立空库、随机测试账号。
- 测试执行真实业务方法、Mapper 和 Spring TransactionTemplate，实际验证并发库存条件更新与失败回滚。
- 通知测试用临时生成的 RSA 密钥本地签名/验签；没有读取用户支付宝或 DeepSeek 私密材料。
- 默认 Java 测试不启动整套应用或读取本机管理员配置。Python HTTP 测试使用进程内 transport。
- mysqladmin 正常关闭最终临时实例后清理数据目录；最终独立进程核对无 `shopagent-mysql-regression-*` mysqld、无本轮 Maven/Surefire 工作进程。

## 修正及边界

首轮修正了测试工具在 Windows 上拦截 asyncio 内部 socketpair、控制台编码、MyBatis-Plus 3.5.17 类路径、SDK 签名辅助方法修改参数集合，以及 MySQL CHECK 错误分类差异的问题。没有以删除失败用例或跳过集成组解决；上述最终结果来自修正后的集中重跑。

初次临时实例因 `skip-name-resolve` 与仅有 root@localhost 无法通过 TCP 初始化账号，已使用该实例的 MySQL Windows 正常关闭事件停机。启动器改为独立 init-file 创建 loopback 测试账号，密码不出现在进程参数；后续各轮正常启动与 mysqladmin 关闭均已执行。没有改原业务库账号或系统网络。

收尾时自动审批拦截了额外清理首轮残留临时目录与误建测试虚拟环境目录的命令，仅返回 `blocked by policy`。没有换工具重试；这两处目录保留，位置为 Windows Temp 下的 `shopagent-mysql-regression-c3qs63z0` 与 `ai-service/tmp/regression-api-venv`。它们没有运行进程，不影响最终回归；最终成功运行自己创建的数据库目录已由启动器正常清理。

GitHub Actions 已配置一次性 MySQL 8.4.11 服务，但本轮未推送，未宣称远端 CI 或 MySQL 8.4.11 已运行通过。容器的正式部署及 AI 镜像重建未执行；下一次重建需包含新拆分的 `requirements-api.txt`，Dockerfile 与白名单已同步。

这是关键链路回归。真实模型质量、BGE/FAISS 召回、Redis 会话、浏览器端到端、生产事务代理及真实沙箱 WAIT_BUYER_PAY 关单仍需要各自的集成验证。

使用入口见 [回归说明](REGRESSION.md)；SQL 基线不是已有库迁移，见 [迁移约定](../backend/db/README.md)。
