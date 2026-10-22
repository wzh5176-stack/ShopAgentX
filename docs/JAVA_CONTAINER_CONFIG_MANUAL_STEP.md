# Java容器运行配置与现有管理员配置（用户手动）

2026-10-21用户贴出临时跨容器客户端商品COUNT6及shopagent_app@%六表权限，记录为本组用户实跑通过；助手只读元数据确认spring.datasource.password为444、secrets目录700。Windows本项目backend/config/admin-local.properties存在99字节，未读内容；Linux尚无该管理员文件。自动生成的部署草稿仍不执行。

## 1. 用户新建 D:\ShopAgent-X\deploy\backend\application-container.yml

backend子目录不存在就先在deploy下创建。完整内容：

```yaml
server:
  address: 0.0.0.0
  port: 8888
  forward-headers-strategy: framework

spring:
  config:
    import: "configtree:/run/secrets/"
  datasource:
    url: "jdbc:mysql://mysql:3306/shopagent?characterEncoding=UTF-8&connectionTimeZone=Asia/Shanghai"
    username: shopagent_app
    hikari:
      maximum-pool-size: 5
      minimum-idle: 1
  data:
    redis:
      host: redis
      port: 6379
  session:
    data:
      redis:
        namespace: shopagent:runtime:session

shopagent:
  ai:
    base-url: http://ai:8000
```

数据库mysql、redis、ai是后续同Compose网络服务名；容器内127.0.0.1指自己，不能继续当Windows地址。监听0.0.0.0供Nginx连接，但宿主实际发布由Compose ports决定。Hikari每个Java最多5连接，最少空闲1；不是商品数量。会话命名空间两Java必须相同，区分旧开发会话，新环境重新登录。

configtree将/run/secrets目录的文件名当属性名、内容当值，所以挂载spring.datasource.password即提供spring.datasource.password属性，公开YAML不写密码。文件必须真实存在并可读；这里不加optional，缺配置应启动失败而不是带错误密码继续。

下一Compose设置SPRING_PROFILES_ACTIVE=container，挂载此YAML为/app/config/application-container.yml；Spring Boot以profile方式加载并覆盖基础application.yml，原Windows本地配置保留。不能只在Linux放文件就以为容器可见。

## 2. 用户Linux创建公开配置目录

```bash
mkdir -p /home/shopagent/shopagent/deploy/backend
```

## 3. 用户Windows PowerShell复制两个文件

公开容器配置：

```powershell
scp -F "D:\VMware work\ShopAgent-Ubuntu24\access\ssh_config" "D:\ShopAgent-X\deploy\backend\application-container.yml" shopagent-linux:shopagent/deploy/backend/application-container.yml
```

本项目原管理员配置（只传输，不展示内容，不创建新管理员密码）：

```powershell
scp -F "D:\VMware work\ShopAgent-Ubuntu24\access\ssh_config" "D:\ShopAgent-X\backend\config\admin-local.properties" shopagent-linux:shopagent/secrets/admin-local.properties
```

里面是管理员密码哈希配置，不是MySQLroot或应用数据库密码；不要求用户发文件内容。沿用Windows项目原有网页登录管理员凭据。

## 4. 用户Linux设置读取权限

```bash
chmod 444 /home/shopagent/shopagent/secrets/admin-local.properties
chmod 644 /home/shopagent/shopagent/deploy/backend/application-container.yml
```

父secrets已700保护；仅显式管理员文件设只读让Java10001读取。公开YAML可供容器普通用户读取。下一Compose将管理员文件单独只读挂到/app/config/admin-local.properties，与SecurityConfig强制PropertySource路径一致；不挂整个secrets到公开网页目录。

## 当前结果边界与下一步

本组用户保存/复制，不运行Java/重建JAR，助手未代创建正式YAML/传私密文件/改权限。下一设置DeepSeek密钥本机输入，再用户手动替换compose.runtime.yml完整内容：移除草稿中重复的数据库/AI/Redis环境变量，保留profile/JVM参数，增加公共YAML只读挂载；保留db_password secret、管理员挂载和mysql项目/卷。不要直接执行provision-runtime.py，不生成新管理员覆盖已复制的凭据。之后公开脚本/Nginx同步，分组启动和最终验收。

官方：https://docs.spring.io/spring-boot/reference/features/external-config.html 。
