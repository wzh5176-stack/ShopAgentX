# Redis Cluster 三主三从演练验收

2026-10-22，独立项目shopagent-cluster-lab。用户手动执行配置上传、六节点启动、组建、写入、停主和恢复；助手只读核对状态与数据，不代执行这些变更。

## 已核实

- 六容器恢复healthy；六节点各自CLUSTER INFO均ok、16384槽assigned/ok、pfail/fail均0、known_nodes6/size3。
- 起始主redis-1、从redis-5；恢复后主redis-5(172.20.0.5)接管0-5460，从redis-1(172.20.0.2)connected，集群epoch从6到7。
- 其余主redis-2→副本redis-6(5461-10922)，主redis-3→副本redis-4(10923-16383)仍connected。
- 三实验键值为product:4=demo-A-after-failover、product:1=demo-B、product:3=demo-C，前缀均shopagent:lab:。
- 助手在恢复节点redis-1同连接READONLY+GET直接读到demo-A-after-failover，确认原主已收到提升后写入的新值，而非仅PING健康。
- 网站runtime六服务healthy、入口healthUP，原共享会话Redis未接入实验集群。

机器可读快照：verification/redis-cluster-failover-2026-10-22.json。

## 结论与边界

分片、主从复制、主节点停止后的角色提升和原主恢复同步已完成演练并有状态/数据佐证。助手未连续观察/计时停机窗口；没有切换耗时测量或吞吐延迟数据。仅单VM的进程故障，不是多物理机容灾；未做断电、分区、绝对零丢失、生产可用性或网站集群接线验收。此配置为内部无宿主端口发布的学习环境。

## 用户下一步：清理练习键并停止实验

Linux进入客户端，以下DEL只针对三条明确的实验键，不使用FLUSHDB/FLUSHALL，三个键跨槽因此逐条删：

```bash
cd /home/shopagent/shopagent/deploy
sudo docker compose -f compose.redis-cluster.yml exec redis-2 redis-cli -c
```

```text
DEL shopagent:lab:product:4
DEL shopagent:lab:product:1
DEL shopagent:lab:product:3
exit
```

每条首次删除通常返回(integer)1。随后：

```bash
sudo docker compose -f compose.redis-cluster.yml stop
sudo docker compose -f compose.redis-cluster.yml ps -a
sudo sh runtime.sh ps
```

期望六实验节点Exited，网站六服务仍healthy。保留数据卷/nodes.conf和配置供复习，不要求把redis-1切回主角色，不删除卷/reset/再次create。2026-10-22更新：用户完成收尾后，助手只读核六实验节点均Exited(0)、网站六healthy；没有重启集群检查删除后的键，因此不把停止状态当三键已不存在的独立证明。

之后补当前Linux备份的隔离恢复验证、少量不调用模型的实际指标、README/演示/贡献说明；不要再堆新业务。

