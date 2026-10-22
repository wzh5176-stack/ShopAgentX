# 独立 Linux MySQL 容器准备（用户手写/手动）

2026-10-21用户备份反馈退出码0，文件D:\ShopAgent-backups\shopagent-20261021-214016.sql为65232字节；SHA256=43CA0D800A4B12D396A8DDC6DABBF5F1B4F0D577652A1C2B3509A2CA57F462DB，助手只读文件元数据/散列一致，未读备份全文或恢复。Linux当次无MySQL容器/3306监听，仅旧Nginx运行、Redis停止；已有旧Redis卷不动。官方mysql8.4.11标签linuxamd64存在已查，恢复8.0.34导出到新8.4实例仍需验证，不挂旧数据库目录自动升级。

## 本组边界

用户先建独立容器与密码，下一组传备份/核SHA/导入并核结构数据。助手仅教案，不创建正式Compose或密码、不复制/启动/登录/导入。密码只保存在Linux工程secrets目录，当前repo外；不索要其内容。

## 1. 用户新建 D:\ShopAgent-X\deploy\compose.mysql.yml

```yaml
name: shopagent-runtime

services:
  mysql:
    image: mysql:8.4.11
    environment:
      MYSQL_DATABASE: shopagent
      MYSQL_ROOT_PASSWORD_FILE: /run/secrets/mysql_root_password
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
    volumes:
      - mysql-data:/var/lib/mysql
    secrets:
      - mysql_root_password
    restart: unless-stopped

volumes:
  mysql-data:

secrets:
  mysql_root_password:
    file: ../secrets/mysql-root-password.txt
```

name设独立Compose项目shopagent-runtime，与旧proxy/session分开；服务名mysql供后续Java容器通过网络DNS连接。镜像带数据库程序，无需容器内手动安装。MYSQL_DATABASE首次初始化建空shopagent，密码FILE让官方入口从secrets文件读root初始密码，不写在公开YAML。command设默认字符集/排序规则；卷保存实际表/数据；重建容器可复用卷。secrets把Linux文件挂成/run/secrets/mysql_root_password，并不是远程密码服务或加密仓库。

未配置ports，当前数据库只在Docker网络内访问；用户通过compose exec在容器内操作，不拿Windows localhost3306连Linux。未配置healthcheck，ps运行不等于SQL初始化完成；后面登录SELECT验证，后续总Compose接应用前再补能认证执行SQL的就绪检查。

根密码/空库变量仅空数据目录首次初始化生效；修改密码文件不会自动修改已有数据库root密码，不删卷解决密码问题。保留今后完整Compose的name、mysql服务名、mysql-data卷标识，应用文件通过同一项目-f合并使用，避免误造第二份库。

## 2. 用户在已登录Linux的SSH标签设置密码

```bash
mkdir -p /home/shopagent/shopagent/secrets
umask 077
read -r -s -p "设置 Linux MySQL root 密码：" shopagentMysqlRootPassword
```

输入自行保存的新Linux数据库root密码后回车（不显示字符），然后：

```bash
printf '\n'
if [ -n "$shopagentMysqlRootPassword" ]; then printf '%s' "$shopagentMysqlRootPassword" > /home/shopagent/shopagent/secrets/mysql-root-password.txt; else printf '密码为空，未写入，请重新输入。\n'; fi
unset shopagentMysqlRootPassword
```

read不在历史写明文；umask让新文件仅本人读写；if拒绝空密码写入，提示为空时先重新输入，不继续启动。密码不发聊天。当前Linux新文件应在首次创建前不存在；若已存在，不盲目覆盖；先反馈状态。

## 3. 用户Windows PowerShell复制公开Compose

```powershell
scp -F "D:\VMware work\ShopAgent-Ubuntu24\access\ssh_config" "D:\ShopAgent-X\deploy\compose.mysql.yml" shopagent-linux:shopagent/deploy/compose.mysql.yml
```

Linux部署目录先前已创建。正式Compose以D盘为源；密码不从本机旧配置复制、不从私密IDE提取。

## 4. 用户Linux启动与登录

```bash
cd /home/shopagent/shopagent/deploy
sudo docker compose -f compose.mysql.yml up -d mysql
sudo docker compose -f compose.mysql.yml logs --tail 30 mysql
```

首次下载与初始化需要时间；等最终数据库就绪，临时初始化服务的启动不当完整就绪。若还初始化稍后再次看日志，不循环拉镜像或删卷。

```bash
sudo docker compose -f compose.mysql.yml exec mysql mysql --user=root --password
```

输入本组刚设置的Linux root密码（不是强制沿用Windows密码）。看到mysql>后执行：

```sql
SELECT VERSION();
SHOW DATABASES;
EXIT;
```

版本预期8.4.11，库列表包含shopagent；不显示或拷贝任何用户密码。此时是空演示库，未导入原表/数据。连接失败保留错误，不把ps Up当完成。当前SQL不写业务数据。

## 下一组

用户整组反馈容器启动/版本与shopagent是否存在。下一Linux建backups→Windows仅scp已知SQL→sha256sum与已记录值对比→compose cp到新mysql容器→交互客户端SOURCE导入shopagent→结构/准确行数/外键与应用最小权限；Windows原库保持。总Compose、HTTP/模型/会话最终运行验收仍后续，不能称完整迁移完成。

依据：https://github.com/docker-library/docs/blob/master/mysql/README.md ，https://docs.docker.com/compose/how-tos/use-secrets/ 。
