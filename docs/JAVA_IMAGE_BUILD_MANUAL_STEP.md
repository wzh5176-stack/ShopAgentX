# 在 Linux 构建 Java 镜像（用户手动）

2026-10-21：用户报告Maven已搞好。只读确认backend/Dockerfile与.dockerignore符合上课代码，target/shopagent-backend-1.0-SNAPSHOT.jar当日20:51更新、64857980字节，manifest为Java17/SpringBoot4.1.1/正确Start-Class。不是本轮助手执行Maven或完整业务验收。以下复制、下载与构建仍由用户操作。

## 1. Linux SSH 标签创建独立构建目录

若未登录，先在Windows新PowerShell执行：

```powershell
ssh -F "D:\VMware work\ShopAgent-Ubuntu24\access\ssh_config" shopagent-linux
```

在已登录Linux的标签执行：

```bash
mkdir -p /home/shopagent/shopagent/build/backend/target
```

专门容纳Dockerfile、dockerignore和JAR，与已有deploy配置目录分开，不替换正在运行的Windows Java。

## 2. 另开Windows PowerShell复制

```powershell
scp -F "D:\VMware work\ShopAgent-Ubuntu24\access\ssh_config" "D:\ShopAgent-X\backend\Dockerfile" "D:\ShopAgent-X\backend\.dockerignore" shopagent-linux:shopagent/build/backend/
```

```powershell
scp -F "D:\VMware work\ShopAgent-Ubuntu24\access\ssh_config" "D:\ShopAgent-X\backend\target\shopagent-backend-1.0-SNAPSHOT.jar" shopagent-linux:shopagent/build/backend/target/
```

保留四通道转发标签，不在其窗口直接输入scp；两条成功再继续。

## 3. Linux检查文件并构建

```bash
cd /home/shopagent/shopagent/build/backend
ls -la
ls -lh target/shopagent-backend-1.0-SNAPSHOT.jar
sudo docker build --pull -t shopagent-backend:dev .
```

ls -la可看到以点开头的dockerignore；文件缺失停在此检查复制。docker build让Linux Docker读取Dockerfile构建，--pull尝试获取基础镜像的当前版本，-t命名shopagent-backend:dev，末尾点是当前目录构建上下文，所以COPY里的target路径才可找到。首次会下载镜像；构建时的RUN创建用户、COPY放入JAR，ENTRYPOINT只是保存启动命令，不会运行商城应用。

完成后：

```bash
sudo docker image ls shopagent-backend:dev
```

预期列出shopagent-backend、dev及镜像ID/大小。不同Docker版本输出格式可能不同，无需与某行英文完全一致。失败时保留具体出错阶段和末尾提示，不自动换源、关TLS或清理现有镜像/卷。

## 学习与后续

JAR是编译好的Java程序及依赖；镜像还带Java运行环境与运行说明；容器是从镜像启动的运行实例。构建镜像不会直接部署商城，现有A/B服务继续使用原来的JAR/IDEA进程。

下一Python镜像准备，然后先核对VM资源、Linux数据库演示库/备份和部署配置，编写总Compose联起Java/Python/DB/Redis/Nginx。管理员配置必须外部只读挂载且用户10001可读；容器server.address/服务地址需覆盖，现不能直接启动Java镜像指望访问Windows回环。上一Nginx故障与代理SSE未明确反馈仍待验。

本课助手未scp、拉镜像、docker build或运行容器。待用户反馈镜像列表后集中核对。

官方：https://docs.docker.com/reference/cli/docker/buildx/build/ 和 https://docs.docker.com/reference/cli/docker/image/ls/ 。
