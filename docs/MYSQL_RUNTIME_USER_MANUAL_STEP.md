# Linux应用账号、表级权限与跨容器登录（用户手动）

2026-10-21用户报告Windows/Linux六表COUNT一致，作为用户反馈记录；助手未认证复核全部结构/约束或数据内容。下一把数据库从管理员操作过渡到Java普通账号，三项合并教学，助手不创建账号/授权/读密码。

## 剩余部署路线

当前镜像、模型离线检索、VM4GB和数据库恢复准备已完成各自范围检查。还需4组：应用账号与密码文件；Java管理员/DB外部配置和Python密钥；完整Compose/容器网络/Nginx新上游；启动与真实业务/流式/登录/停实例回归。按手动讲解粗估顺利1～2小时，仅估算，排错另计，RedisCluster不计入此次部署时间。不按“COUNT一致”称数据库完整一致性已独立验收。

## 1. 用户Linux进入root客户端

```bash
cd /home/shopagent/shopagent/deploy
sudo docker compose -f compose.mysql.yml exec mysql mysql --user=root --password
```

输入Linux root密码。mysql>执行：

```sql
CREATE USER 'shopagent_app'@'%' IDENTIFIED BY RANDOM PASSWORD;
```

结果generated password列为新应用密码，用户自行保存，仅复制该值不带表格字符，不发送聊天/截图。若账号已存在报错则停，不DROP/ALTER或IFNOTEXISTS掩盖密码不匹配。

然后完整执行与Windows现有授权一致的表级权限：

```sql
GRANT SELECT, INSERT, UPDATE
ON shopagent.products TO 'shopagent_app'@'%';

GRANT SELECT, INSERT
ON shopagent.app_users TO 'shopagent_app'@'%';

GRANT SELECT, INSERT, UPDATE
ON shopagent.chat_conversations TO 'shopagent_app'@'%';

GRANT SELECT, INSERT
ON shopagent.chat_messages TO 'shopagent_app'@'%';

GRANT SELECT, INSERT, UPDATE, DELETE
ON shopagent.cart_items TO 'shopagent_app'@'%';

GRANT SELECT, INSERT, UPDATE
ON shopagent.cart_actions TO 'shopagent_app'@'%';

SHOW GRANTS FOR 'shopagent_app'@'%';
EXIT;
```

root只管理部署，不给Java用。账号host部分%允许来自容器网络的连接，不表示任意数据库/任意操作；当前MySQL未发布宿主3306，仅网络成员可达。产品逻辑删除UPDATE所以无DELETE；聊天消息仅SELECT/INSERT保留原边界；购物车物理移除需DELETE。GRANT立即生效不需要FLUSH PRIVILEGES；Linux账号独立于Windows@127.0.0.1。

## 2. 用户Linux保存生成的应用密码

```bash
umask 077
read -r -s -p "粘贴刚才生成的应用数据库密码：" shopagentDbPassword
```

只粘密码值按回车后再执行后面代码，勿把后续命令一起粘给read消费。

```bash
printf '\n'
if [ -n "$shopagentDbPassword" ]; then printf '%s' "$shopagentDbPassword" > /home/shopagent/shopagent/secrets/spring.datasource.password; else printf '密码为空，未保存，请重新输入。\n'; fi
unset shopagentDbPassword
```

新文件是Spring配置属性名，后续configtree挂载让Java读取；当前只保存，不启动Java。仅初次新建使用，若既有文件/账号则先核状态不覆盖。当前umask077的600文件对Java10001不可直接读，下一外部配置组需要在secrets父目录700保护下为该特定文件设置容器可读权限（如444），或其他明确ACL；不能仅依赖Compose秘密mode字段假设本地bind更改属主有效。助手不读取密码文件。

## 3. 用户用临时客户端测试容器网络

```bash
sudo docker run --rm -it --network shopagent-runtime_default mysql:8.4.11 mysql --host=mysql --user=shopagent_app --password shopagent
```

官方镜像这次运行mysql客户端，不创建另一数据库服务；--network加入现有项目默认网络，mysql主机名解析到数据库容器。输入应用密码（不是root）。mysql>执行：

```sql
SELECT CURRENT_USER();
SELECT COUNT(*) FROM products;
SHOW GRANTS;
EXIT;
```

预期CURRENT_USER为shopagent_app@%，可读商品数量并查看自己的表级权限。证明同网络另一个容器可认证读库，不证明Java已有配置或完整写入/权限拒绝通过。临时容器退出删除，数据库卷/服务保留。

## 结果与下一组

用户只反馈应用登录/读商品是否成功、权限异常，不能发送创建用户返回的密码。助手未代执行。下一Java外部管理员哈希配置、DB配置树与用户10001权限，Python运行环境密钥配置；再完整Compose接Redis/两个Java/AI/Nginx，保留shopagent-runtime项目/mysql-data卷及MySQL镜像版本，不重建恢复库。后续需集中结构/约束/真实事务/HTTP验证，旧Nginx故障/SSE尚待最终补齐。

依据：https://dev.mysql.com/doc/refman/8.4/en/create-user.html ，https://dev.mysql.com/doc/refman/8.4/en/grant.html 。
