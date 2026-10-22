# ShopAgent Linux 演示环境运行手册

更新：2026-10-22。适用 Ubuntu VMware 单机、shopagent-runtime 项目；全部应用运行在 Linux，Windows 只负责浏览器与 SSH 转发。

## 本轮验收记录

| 内容 | 结果与证据来源 |
| --- | --- |
| 六容器健康、Nginx8080健康接口 | 助手SSH只读确认全部healthy，HTTP成功/UP |
| 真实导购、多轮预算、逐段回答、历史恢复 | 用户逐组反馈可用；本轮未由助手独立浏览器复验 |
| 推荐来源操作单、人工确认、购物车与操作历史恢复 | 用户逐组反馈可用；助手未代写业务数据 |
| K1演示资料问答与来源引用 | 用户反馈可用；资料为虚构教学内容 |
| 停A后同浏览器身份/商品/购物车，恢复A | 用户反馈通过；随后助手只读确认A恢复healthy、B及其余健康，不能视为助手独立观察停机窗口 |
| 独立Redis Cluster三主三从 | 2026-10-22分片/复制/停主提升/恢复同步已核，六视图ok全槽；详REDIS_CLUSTER_ACCEPTANCE.md。网站仍使用原Redis |
| Linux现状SQL隔离恢复 | 2026-10-22六恢复表及数据可读，结构与11项关联检查通过；详MYSQL_RESTORE_CHECK_ACCEPTANCE.md，未做网站切换或恢复测时 |
| TLS信任调整 | Windows57根证书导出，AI专用只读挂载与合并CA脚本；用户反馈真实请求恢复。不称间歇错误已彻底根治 |

这是单VM应用实例接替演练。VM、MySQL、Redis、Nginx本身仍为单点；不保证已开始的SSE自动续传、写请求自动重放或生产可用性。未进行压测。本次轻载资源快照：Linux3.8Gi总内存/1.7Gi已用/2.1Gi可用、无swap，根盘29G/可用20G，仅当次观察。

## 日常启动

先关闭eNSP设备，再在VMware启动ShopAgent-Ubuntu24，等待Linux启动完成。不要更改Windows虚拟化功能。

Windows PowerShell连接（若刚开机SSH暂时超时，等待Linux就绪再试）：

```powershell
ssh -F "D:\VMware work\ShopAgent-Ubuntu24\access\ssh_config" shopagent-linux
```

Linux：

```bash
cd /home/shopagent/shopagent/deploy
sudo sh runtime.sh up -d --wait --wait-timeout 240
sudo sh runtime.sh ps
```

另开Windows PowerShell标签并保持运行：

```powershell
ssh -F "D:\VMware work\ShopAgent-Ubuntu24\access\ssh_config" -N -T -L 127.0.0.1:8080:127.0.0.1:8080 -o ExitOnForwardFailure=yes -o ServerAliveInterval=15 -o ServerAliveCountMax=3 shopagent-linux
```

浏览器：http://127.0.0.1:8080/shopagent/home.html 。不需要启动WindowsIDEA/PyCharm内的业务服务。

## 日常停止

Linux：

```bash
cd /home/shopagent/shopagent/deploy
sudo sh runtime.sh stop
sudo shutdown -h now
```

SSH断开且VMware显示虚拟机关闭电源后，可以关闭其余窗口。stop保留容器和数据卷，不能以down -v替代。只关终端不会关VM和容器。

## 查看状态与错误

Linux：

```bash
cd /home/shopagent/shopagent/deploy
sudo sh runtime.sh ps -a
curl -i --max-time 10 http://127.0.0.1:8080/api/health
sudo sh runtime.sh logs --tail 80 ai
sudo sh runtime.sh logs --tail 80 backend-a backend-b
```

日志本地查看，发送前去除可能的个人内容/凭证；不发送密钥或Cookie。healthUP不表示DeepSeek实时可用。网页连不上先区分VM/SSH、8080转发、容器、真实供应商请求，不先修改密码。TLS修复详RUNTIME_TLS_MANUAL_FIX.md。

## 手动备份当前Linux业务数据库

2026-10-22更新：用户已执行下面导出，助手SSH只读核实/home/shopagent/shopagent/backups/shopagent-runtime-20261022-162816.sql为67040字节、权限600，SHA256为676086d726c43dc73cd3726b72c6dc6505f17e916bf964175ea9530c9abb554c。D盘同名文件已下载并核SHA相同；本份SQL已在独立无网络MySQL恢复六表并核结构/关联，详MYSQL_RESTORE_CHECK_ACCEPTANCE.md。下列命令保留供以后备份使用，不要求重新导出或导入。

2026-10-21的D盘SQL是迁移时Windows源库快照，不包含之后Linux中新聊天/购物车改动。现在需从Linux mysql服务重新备份。备份包含账号散列与业务隐私，只保存在私密备份目录，不入Git。

保持服务运行，避免备份过程中改表结构；六业务表为InnoDB。Linux逐块执行：

```bash
cd /home/shopagent/shopagent/deploy
mkdir -p /home/shopagent/shopagent/backups
chmod 700 /home/shopagent/shopagent/backups
umask 077
shopagent_backup_file="/home/shopagent/shopagent/backups/shopagent-runtime-$(date +%Y%m%d-%H%M%S).sql"
```

下面整个if块一次执行。root密码只在容器内从现有secret传给客户端，不显示密码值；SQL写宿主机partial文件，成功才更名：

```bash
if sudo sh runtime.sh exec -T mysql sh -c 'export MYSQL_PWD="$(cat /run/secrets/mysql_root_password)"; exec mysqldump --user=root --single-transaction --quick --no-tablespaces --set-gtid-purged=OFF --default-character-set=utf8mb4 --hex-blob --routines --events --triggers shopagent' > "${shopagent_backup_file}.partial"
then
    mv -- "${shopagent_backup_file}.partial" "$shopagent_backup_file"
    ls -lh -- "$shopagent_backup_file"
    sha256sum -- "$shopagent_backup_file"
else
    printf '备份失败，保留partial文件供检查，不作为有效备份。\n' >&2
fi
```

不读取SQL内容。成功后记录完整路径/大小/SHA；下一用scp将这一个具体文件下载D:\ShopAgent-backups，再用Get-FileHash核传输一致性。单文件与hash只证明导出/传输，不证明可以恢复。

## 恢复与回退边界

恢复应在新的隔离测试MySQL/空库进行：先确认目标、核SHA、导入、核结构/六表数据与业务回读，再决定切换。本次SQL已完成隔离恢复及列明的检查，但没有把网站切换到恢复库或测量恢复耗时；不要把旧MYSQL_RESTORE_MANUAL_STEP.md中的迁移导入命令直接重放到现有业务库。备份不含MySQL系统账号授权、外部秘密文件、模型文件或所有项目源码；这些需分别保管。秘密只在本机私密位置保存，不发聊天、不入Git。

当前镜像使用dev标签，没有形成版本化发布回退包；旧配置可供比较，但不能声称任意新旧版本一键回退已经验收。

## 后续路线

2026-10-22现状SQL导出/D盘SHA及隔离恢复结构/关联检查通过，独立RedisCluster与恢复实验均已停止。商品接口基线已由用户运行并归档PRODUCT_BASELINE_REPORT.md，根README/DEMO_AND_REVIEW/CONTRIBUTION_AND_RESUME已保存。下一默认演示与源码复盘，网站正常保留；LangGraph图编排、订单支付与物流仍未实现，不写成已完成能力。

## Redis Cluster复习启动与停止

Linux在deploy目录，用指定-f compose.redis-cluster.yml up -d --wait --wait-timeout 120启动已有节点，之后CLUSTER INFO检查拓扑收敛；用同一-f的stop停止。不再执行--cluster create，不需要把原主redis-1强行切回主；恢复后角色以实际ROLE/NODES为准。IP是Docker当次地址，重建后须重核。实验配置restart为no，重启VM不会自动启动实验，网站启动流程不变。
