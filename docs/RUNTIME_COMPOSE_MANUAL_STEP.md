# 完整运行编排：用户手动替换Compose、复制与静态检查

2026-10-21用户继续。只读确认Linux四个秘密文件名/权限均存在（未读值、未验证APIKey有效性），Java容器YAML和start-ai脚本已在；仅独立MySQL与旧Nginx运行，8080由旧Nginx占用。真实model_probe结果尚未反馈。助手仅教案，不代写正式Compose或执行复制/启动。

## 1. 用户完整替换 D:\ShopAgent-X\deploy\compose.runtime.yml

此文件曾自动准备过草稿，现改为挂外部YAML，移除重复DB/Redis/AI地址环境变量。完整内容：

```yaml
name: shopagent-runtime

x-backend: &backend
  image: shopagent-backend:dev
  pull_policy: never
  restart: unless-stopped
  mem_limit: 640m
  environment:
    SPRING_PROFILES_ACTIVE: container
    JAVA_TOOL_OPTIONS: -Xms64m -Xmx256m -XX:MaxMetaspaceSize=192m
  secrets:
    - source: db_password
      target: spring.datasource.password
  volumes:
    - ./backend/application-container.yml:/app/config/application-container.yml:ro
    - ../secrets/admin-local.properties:/app/config/admin-local.properties:ro
  depends_on:
    mysql:
      condition: service_healthy
    redis:
      condition: service_healthy
    ai:
      condition: service_healthy
  healthcheck:
    test: ["CMD", "curl", "--fail", "--silent", "http://127.0.0.1:8888/api/health"]
    interval: 10s
    timeout: 5s
    retries: 12
    start_period: 60s

services:
  mysql:
    mem_limit: 768m
    healthcheck:
      test: ["CMD-SHELL", "MYSQL_PWD=\"$$(cat /run/secrets/mysql_root_password)\" mysql --host=127.0.0.1 --user=root --execute='SELECT 1' >/dev/null 2>&1"]
      interval: 10s
      timeout: 5s
      retries: 20
      start_period: 40s

  redis:
    image: redis:7.4.11-alpine
    restart: unless-stopped
    mem_limit: 192m
    command: ["redis-server", "--appendonly", "yes"]
    volumes:
      - runtime-redis-data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 12

  ai:
    image: shopagent-ai:dev
    pull_policy: never
    restart: unless-stopped
    mem_limit: 1400m
    environment:
      DEEPSEEK_API_KEY_FILE: /run/secrets/deepseek_api_key
      HF_HUB_OFFLINE: "1"
      TOKENIZERS_PARALLELISM: "false"
      OMP_NUM_THREADS: "1"
      OPENBLAS_NUM_THREADS: "1"
      MKL_NUM_THREADS: "1"
    entrypoint: ["/bin/sh", "/opt/shopagent/start-ai.sh"]
    command: ["python", "-m", "uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
    volumes:
      - ./scripts/start-ai.sh:/opt/shopagent/start-ai.sh:ro
      - ../models/bge-small-zh-v1.5:/app/model_cache/bge-small-zh-v1.5:ro
    secrets:
      - deepseek_api_key
    healthcheck:
      test: ["CMD", "python", "-c", "import urllib.request; urllib.request.urlopen('http://127.0.0.1:8000/health', timeout=3).read()"]
      interval: 10s
      timeout: 5s
      retries: 20
      start_period: 45s

  backend-a:
    <<: *backend

  backend-b:
    <<: *backend

  nginx:
    image: nginx:1.30.2-alpine
    restart: unless-stopped
    mem_limit: 64m
    ports:
      - "127.0.0.1:8080:8080"
    volumes:
      - ./nginx/nginx.runtime.conf:/etc/nginx/nginx.conf:ro
    depends_on:
      backend-a:
        condition: service_healthy
      backend-b:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "wget", "-q", "-O", "/dev/null", "http://127.0.0.1:8080/api/health"]
      interval: 15s
      timeout: 5s
      retries: 8

volumes:
  runtime-redis-data:

secrets:
  db_password:
    file: ../secrets/spring.datasource.password
  deepseek_api_key:
    file: ../secrets/deepseek-api-key.txt
```

## 配置解析

- x-backend/&backend与<<:*backend是YAML复用模板，不额外启动第三个Java；A/B是两个容器，均8888不冲突。
- profile=container加载/app/config/application-container.yml与基础配置合并；secret target名供configtree读密码；管理员仍独立PropertySource挂载，不重建哈希。
- pull_policy never要求使用本地两个dev镜像，不因缺失去拉不存在的公开镜像。
- mysql条目合并compose.mysql.yml已有镜像/卷/secret，只加资源与认证SQL健康检查；必须同时加载两文件且原项目名/卷不改，复用恢复数据。
- $$留给容器shell执行，cat读取rootsecret只供health认证，不打印密码；SELECT1检查能认证执行SQL。
- depends_on service_healthy控制启动时依赖就绪，并不意味着运行中自动重启依赖服务或实现容灾。
- Redis使用新runtime卷，旧session卷保留；新命名空间/新Redis应重新登录。
- AI使用已有镜像/本地模型/secret及新脚本，health只验证HTTP可达，不证明真实供应商或RAG已通过。
- nginx唯一发布Linux1278080，A/B和DB不对宿主发布。mem_limit为上限不预留，4GB是否足够以实际占用/OOM和响应验证为准，非生产性能结论。

## 2. 另外两个已有公开草稿直接复用

无需本组改内容：D:\ShopAgent-X\deploy\runtime.sh，以及D:\ShopAgent-X\deploy\nginx\nginx.runtime.conf。前者合并mysql/runtime两份Compose；后者用backend-a:8888与backend-b:8888，不再用开发用SSH反向18888/18889。Nginx通过Docker127.0.0.11 DNS及resolve/zone应对容器IP变化；本组未实际运行nginx-t。

## 3. 用户Windows PowerShell复制

```powershell
scp -F "D:\VMware work\ShopAgent-Ubuntu24\access\ssh_config" "D:\ShopAgent-X\deploy\compose.runtime.yml" "D:\ShopAgent-X\deploy\runtime.sh" shopagent-linux:shopagent/deploy/
scp -F "D:\VMware work\ShopAgent-Ubuntu24\access\ssh_config" "D:\ShopAgent-X\deploy\nginx\nginx.runtime.conf" shopagent-linux:shopagent/deploy/nginx/
```

三个文件LF换行。未复制私密内容到仓库。

## 4. 用户Linux静态检查

```bash
cd /home/shopagent/shopagent/deploy
chmod 644 compose.runtime.yml runtime.sh nginx/nginx.runtime.conf
sudo sh runtime.sh config --quiet
echo $?
```

无错误/退出码0只表示配置可解析，不代表服务运行或文件内密码有效。runtime.sh内容为进入自身目录后exec docker compose -f compose.mysql.yml -f compose.runtime.yml "$@"，使用sudo sh无需可执行位。检查失败保留完整错误，不进入启动。

## 5. 用户静态检查成功后启动

仅在前面退出码0后，Linux继续：

```bash
sudo docker compose -f compose.nginx.yml stop nginx
sudo sh runtime.sh up -d --wait --wait-timeout 240
sudo sh runtime.sh ps
curl -fsS --max-time 10 http://127.0.0.1:8080/api/health
```

第一条仅停旧代理腾出8080，不停MySQL；up会按健康依赖启动六个服务，等待最多240秒。出现报错先保留错误，不继续修改/重试多套配置。MySQL可因新增health/资源设置被重建容器但保留现有mysql-data卷。预期runtime ps六服务healthy、health返回UP；健康检查只基础可达，不算真实业务整体验收。禁止down -v/删卷/重导凑结果；不运行provision-runtime.py。下一Windows仅8080正向转发、网页/登录/SSE/RAG/购物车/故障恢复集中验证。真实model_probe结果仍待用户，不把Key文件存在记真实模型通过。助手未代写正式runtime配置、scp、停止服务或up。

依据：https://docs.docker.com/compose/how-tos/multiple-compose-files/merge/ ，https://docs.docker.com/compose/how-tos/startup-order/ ，https://nginx.org/en/docs/http/ngx_http_upstream_module.html#server 。
