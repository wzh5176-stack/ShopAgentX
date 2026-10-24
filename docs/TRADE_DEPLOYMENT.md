# 交易版部署与验证

本项目使用 Java + Python 独立服务、MySQL、Redis 共享会话与 Nginx 双 Java 实例。支付使用支付宝官方沙箱；没有接入真实收款、退款或履约系统。

## 配置与启动

原部署文件 `compose.mysql.yml` 与 `compose.runtime.yml` 保留。交易版增加 `compose.trade.yml`；已有环境通过 `.trade-runtime-enabled` 标记选择交易配置，`runtime.sh` 会自动加载。

运行前需自行准备数据库、管理员及沙箱材料，均放在 Git 忽略的 `secrets/` 下。沙箱私钥、公钥、应用配置通过只读挂载进入 Java；公开仓库没有密钥或数据库备份。容器账号为 `shopagent_app@%`，本机 SQL 中的 `@127.0.0.1` 授权不能直接作为容器授权使用。

将 `deploy/trade.env.example` 复制为 `deploy/.env.trade.local`，填入自己的 HTTPS 通知地址。路径必须是 `/api/payments/alipay/notify`。该文件被 Git 忽略；公开 Compose 只引用环境变量，不保存当前运行域名。

Linux 已准备环境：

```bash
cd /home/shopagent/shopagent/deploy
sudo sh runtime.sh up -d --wait
sudo sh runtime.sh ps
```

本地 Windows + VMware 配套入口：

```powershell
& "D:\ShopAgent-X\deploy\start-trade.ps1"
# 需要临时公网通知时：
& "D:\ShopAgent-X\deploy\start-trade.ps1" -PublicNotify
```

脚本使用已有 SSH 配置与 Ubuntu，不安装虚拟机、不切换系统虚拟化。仓库中的 Windows/SSH 路径是作者开发环境示例，其他机器需调整。当前商城入口为 `http://127.0.0.1:8080/shopagent/home.html`。

停止：

```powershell
& "D:\ShopAgent-X\deploy\stop-trade.ps1"
```

## 数据迁移与回退

交易版新增地址、库存、订单、订单明细、支付尝试、模拟支付回执六表。在真实旧库上先备份并隔离恢复验证，再按缺失结构迁移、初始化缺失库存并补最小权限，保留已有用户、商品、聊天和购物车。

`backend/sql/017`—`027` 是结构与授权脚本，不应无条件重复执行。`deploy/scripts/trade-runtime-maintenance.py` 是本次迁移辅助工具：它会拒绝已存在部分交易表的环境，不能视为通用自动迁移框架。`trade-schema-20261024.json` 只含已验证结构，不含本机业务数据。

旧镜像及配置副本保留。取消交易版激活标记后可使用原 Compose 配置，新增表可保留；本次没有实际切回旧应用进行回退测试。数据库恢复属于独立操作，不应把 Windows 测试库覆盖进 Linux。

## 已验证范围

- Java17/Maven打包成功，双实例运行同一 JAR；MySQL、Redis、AI、双 Java、Nginx 六容器健康。
- 原六表旧数据指纹不变；备份在无网络的隔离 MySQL 内恢复并逐表比对通过。
- 29项API与业务检查覆盖普通登录、共享会话、CSRF、地址、可信核价、幂等建单、普通取消、沙箱开关、通知拒绝、真实推荐与知识问答。
- 真实 Edge 浏览器检查桌面1440与手机390页面，无横向溢出和脚本异常。
- Linux 容器使用生产 SDK 配置与查询服务，真实只读查到此前沙箱已支付交易，TLS 验证保持开启。
- 正向沙箱付款通知、漏通知后的查单补偿已在 Windows 由用户真实操作验证；Linux 通知入口验证了公网可达及无签名请求被拒绝，没有代用户再发起新付款。

真实 `WAIT_BUYER_PAY` 交易的主动官方关单仍未实测，不能把 `NOT_FOUND` 链接过期后的库存回收当成该分支通过。最终启动脚本的 Pinggy 冷重连和停机逻辑仅完成语法检查，没有实测全部生命周期。完整原始验收记录与本机操作流水保留在作者本地，不随公开仓库上传。

临时通知隧道使用 Pinggy 免费会话，最长60分钟，重连后域名变化；启动脚本将新地址写入忽略的本地配置。普通浏览与自动查单不依赖此通知通道。该入口用于开发测试，不是生产固定域名。参见 [Pinggy说明](https://pinggy.io/help/)。
