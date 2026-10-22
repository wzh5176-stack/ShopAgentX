# 当前Linux备份的隔离恢复验收

日期：2026-10-22。用户手动导入；助手只读检查实际恢复数据库。机器可读记录见verification/mysql-restore-check-2026-10-22.json。

## 目标与结果

备份shopagent-runtime-20261022-162816.sql，67040字节，SHA256=676086d726c43dc73cd3726b72c6dc6505f17e916bf964175ea9530c9abb554c，Linux与D盘一致。目标为独立shopagent-restore-lab/mysql-restore，MySQL8.4.11、shopagent库、hostname22def9c1e91c，networknone/无发布端口/独立数据卷，导入前空库已独立确认。

用户未贴导入退出码；不能补写“已确认导入退出0”。但随后六表和数据实际存在，助手独立读取核实：

| 表 | 恢复行数 |
| --- | ---: |
| products | 6 |
| app_users | 11 |
| chat_conversations | 31 |
| chat_messages | 167 |
| cart_items | 1 |
| cart_actions | 35 |

六表均InnoDB、utf8mb4_unicode_ci；6主键、3唯一约束、9外键、17CHECK约束存在。11项只读关联检查均0异常：会话用户、消息会话、购物车用户/商品、操作单用户/会话/来源消息/商品、工具消息操作单、操作单归属与来源推荐、工具消息和操作单同会话。普通商品id/价格/状态/逻辑删除可读；未读取账户散列或对话正文。

首次SHOW CREATE的客户端字符集将中文显示成问号；改为显式utf8mb4并HEX检查确认存储的中文商品名与表注释仍为正确UTF-8字节，不据显示结果认定数据损坏。无需修改数据。

网站runtime六服务healthy、入口healthUP；助手未修改在线库或将网站接到恢复库，未重新导入或读取SQL正文。

## 验收结论与边界

这份逻辑备份已在隔离空库恢复出六业务表和可读取数据，结构及所列引用一致性检查通过。未逐行解析原SQL做全量字节比较、未给网站切换连接、未测试恢复账号登录、未测恢复耗时；当前在线库可能在备份后继续变化，不以在线新行数直接判恢复失败。不得把导出/传输/此恢复演练说成完整灾备切换已经完成。

## 用户停止临时恢复容器

Linux：

```bash
cd /home/shopagent/shopagent/deploy
sudo docker compose -f compose.restore-check.yml stop
sudo docker compose -f compose.restore-check.yml ps -a
```

应mysql-restore Exited。停止保留恢复数据卷供复查，其中含备份恢复的私密业务数据；不会替换网站MySQL数据卷。2026-10-22更新：用户已停止，助手只读复核Exited(0)，网站仍六healthy。下列后续指标/文档现已归档，见PRODUCT_BASELINE_REPORT.md与根README。

下一少量不调用模型的接口实测（环境、次数/并发、耗时与局限写清），然后README、演示流程、贡献说明收尾。集群和恢复演练不再重复创建/导入。

