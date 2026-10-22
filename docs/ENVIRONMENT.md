# ShopAgent 本地环境检查

2026-10-22当前：网站runtime六服务healthy/healthUP，双Java/共享Session/真实网页链已反馈通过；独立Cluster与restore实验均Exited。VM2vCPU/配置4GiB，内核6.8.0-147-generic，宿主Python3.12.3；Java17/Boot4.1.1/MyBatisPlus3.5.17、MySQL8.4.11/Redis7.4.11/Nginx1.30.2。用户商品基线30串行mean9.89/P9514.94ms，见PRODUCT_BASELINE_REPORT.md，不是容量测试。根README和RUNTIME_RUNBOOK提供当前启动/停止步骤，旧开发连接和下方待验说明为历史快照；不自动改eNSP/虚拟化/私密配置。

2026-10-21最新：VMware Ubuntu内存已由用户调4096MB，完整LinuxCompose六服务MySQL8.4.11/Redis7.4.11/AI与两个Java本地dev镜像/Nginx1.30.2全部healthy，独立HTTP分发/首页/商品只读通过。Linux只发布127.0.0.1:8080；内部MySQL3306、Redis6379、AI8000、两Java各8888。旧开发proxy和session容器停止，旧Redis卷与Windows数据库保留；应用不再依赖WindowsIDEA/PyCharm服务。Windows8080当次无监听，用户下一建仅L8080的SSH浏览器访问通道。模型/数据库/管理员配置在Linux仓库外secrets，以单文件只读挂载；不写值于本文。资源仅轻载快照约1.7/3.8GiB，完整浏览器/真实AI/故障待验，见RUNTIME_WEB_MANUAL_STEP.md与verification/runtime-readonly-2026-10-21.json。用户手动优先，不自动停VM或改eNSP/Windows虚拟化。

2026-10-20休息关机：按用户明确请求，已停止记录归属的Redis SSH转发，SSH正常关闭ShopAgent Linux，再退出VMware图形窗口/托盘。最终vmrun显示0台、无vmware-vmx/VMware UI、Windows6379无监听；未删除镜像/虚拟盘/数据卷，未停止其他项目服务或修改Windows功能。手动恢复教案REDIS_VMWARE_MANUAL_RECAP.md。此前Redis环境成功证据保留，但当前已停止；新增“转发清理→软关机”停止链这次实际通过。今后环境步骤也先教用户手动操作，不默认代启动。

2026-10-20 Redis VMware环境就绪：Linux Docker29.9.1/Compose5.6.0，容器shopagent-session-redis-1、Redis7.4.11、命名卷shopagent-session_redis-session-data、AOF=yes、healthy。Linux127.0.0.1:6379经隐藏SSH专用转发到Windows127.0.0.1:6379（本次PID15528，恢复时核记录/进程），Windows实测PONG及临时SET/GET/TTL/清理通过，VM NAT6379不可直连。用户原Compose两端哈希一致；未改正式文件或Windows功能。说明REDIS_VMWARE_ENVIRONMENT_STEP.md、证据verification/redis-vmware-environment-2026-10-20.json。Linux/Redis/转发运行，eNSP保持关闭；Java应用会话验收未做。启动脚本Start-ShopAgentRedis.ps1重复调用通过，停止清理链未执行；Linux重启后须compose up而非假定容器自动恢复。

2026-10-20 VMware环境已按新请求实施：新ShopAgent-Ubuntu24（VMX位于D:\VMware work\ShopAgent-Ubuntu24\vm\ShopAgent-Ubuntu24），Ubuntu24.04.5/2vCPU/2GB/30GB，已有VMnet8 NAT未改，检查时192.168.100.128。官方OVA SHA256验证、cloud-init、Windows SSH、HTTPS、根分区扩展与时间同步通过，已软关机再启动。详VMWARE_LINUX_SETUP.md及verification/vmware-linux-2026-10-20.json。eNSP/VirtualBox残留检查为空，Windows Hypervisor仍false；原CentOS7未动。VMware Tools服务active但vmrun取IP报状态错误，使用租约核MAC+SSH。没有Redis/Docker/Nginx安装或Java/Python修改，下一Redis连接准备；不能在Linux运行时重开eNSP。

2026-10-20 21:37 eNSP环境约束与待命：本次只读确认BIOS虚拟化开启、Windows Hypervisor未运行、VirtualBox5.2.26、VMware17.6.4。发现已登记CentOS7虚拟机配置（2vCPU/4GB/NAT），未启动或连接客户机；vmrun与SSH客户端存在不代表已连通。eNSP相关进程运行，用户暂需继续实验。详细快照、冲突解释与资料见ENSP_VIRTUALIZATION_NOTES.md。旧Docker默认启动安排暂停；不改Windows功能/BCD/注册表/网卡，不关闭eNSP，不启动Docker或VMware虚拟机。新Linux/Redis部署尚未实施，后续重新核验即时状态。

2026-10-20会话共享课环境准备：6379无监听；通过docker desktop start --detach成功请求启动，出现DockerDesktop/backend进程，docker info 12秒探针超时，尚未确认引擎就绪。未创建容器/下载Redis镜像/修改正式Compose/POM/配置或编译。专用本机Redis配置完整候选在SESSION_REDIS_MANUAL_STEPS.md，由用户手写，写好后助手负责环境与集中验收；现有Java26008和用户Python25836保留。见verification/session-redis-environment-2026-10-20.json。

2026-10-20真实联调与新知识页：正常Java PID26008/127.0.0.1:8888运行，用户原PyCharm Python PID25836/8000保留，MySQL PID8148/3306保留；旧4416不在运行，未误停其他服务。原密钥仅由Python自身配置使用，未读值/IDE配置。正式131源码package成功；实际JAR/真实模型27检查、最终打包25受控浏览器边界和4只读复核通过。临时Java/浏览器/桥接全停，没有停止用户Python。启动清单tmp/knowledge-web-audit/normal-preview-2026-10-20.json，知识页http://127.0.0.1:8888/shopagent/knowledge.html。Docker CLI位于用户本地DockerDesktop/resources/bin，docker info失败（dockerDesktopLinuxEngine管道不存在），未由助手启动引擎；阶段8仅只读体检，未装Redis或部署容器，证据stage8-environment-2026-10-20.json。

2026-10-20商城补页运行状态：正常Java PID4416、127.0.0.1:8888运行，入口http://127.0.0.1:8888/shopagent/home.html；原MySQL PID7968/3306保留。前组正常Java18300及本轮中间20960已核对项目路径后停止用于重打包，其他进程不动。正式131源码Maven package成功（skipTests），48技术检查使用临时实际JAR/Edge/MySQL，无Python/模型调用，临时进程均停止；最终CSS构建另18正常只读检查通过。终端Key存在性false，未读值/IDE私密配置，8000留给原PyCharm配置。启动清单tmp/storefront-audit/normal-preview-storefront-2026-10-20.json，正式资源与JAR散列见verification/storefront-assets-2026-10-20.json。

2026-10-20网页集中验收：正式131Java package、最终36检查通过，实际Edge/正式JAR/真实MySQL与Python，模型供应商受控且付费0，四个页面资源均与打包内容一致。临时进程全部停止；正常预览Java PID18300/8888运行，原MySQL PID7968/3306保留。终端仅检测DEEPSEEK_API_KEY存在性=false，未读密钥值/IDE配置，因此未占用8000，真实AI需原PyCharm配置。正常URL http://127.0.0.1:8888/shopagent/play.html；启动记录tmp/web-workspace-audit/normal-preview-2026-10-20.json，日志normal-java.log。服务归属核对后只停止本项目拥有的进程；现有其他服务不动。见WEB_WORKSPACE_ACCEPTANCE.md。

2026-10-19工具历史三组正式验收：实际MySQL8.0.34确认016列/唯一/FK/CHECK已执行，消息表仍仅SELECT/INSERT；131Java构建、226技术检查通过，未改正式代码或权限。临时工具在tmp/tool-message-formal-audit，全拥有进程已停，8000/8888停、MySQL PID7436/3306保留。受控供应商HTTP/流，付费调用0。保留消息144～167和4测试操作单（1成功3取消），cart已清空，原业务数据基线保持，仅练习会话时间正常变化。

2026-10-19分流组验收：正式130Java package成功；实际JAR+MySQL+实际Python（模型供应商HTTP/流受控）128技术检查全通过。临时工具位于tmp/conversation-turn-formal-audit，所有本轮拥有的服务均停止，正常8000/8888停，3306原MySQL PID7436保留。无正式源码/依赖/授权变更、付费调用0。保留练习消息107～116及2测试操作单，cart已清空；原数据基线一致，仅练习会话updated_at随新增消息正常变化。详CONVERSATION_TURN_ACCEPTANCE.md。

2026-10-19购物车操作单整组验收收尾：正式126Java源码package成功；实际JAR/真实MySQL等154项技术检查通过，未改应用源码/依赖/权限。临时工具在tmp/cart-action-formal-audit；本轮拥有的Java/Python全停，正常8000/8888停，MySQL PID7436/3306保留。付费模型调用0（prepare回归使用受控模型HTTP）；未读取私密Key配置。测试cart已清空，保留14条声明测试操作记录，原业务基线一致。详CART_ACTION_ACCEPTANCE.md和verification两份cart-action报告。

2026-10-19工具意图整组：使用ai-service/.venv实际解释器修复cart_tool_shema.py命名为cart_tool_schema.py，main实际import成功；120Java package/Python51/Java和整链102通过。终端模型Key有无=false，不读值/IDE私密配置；模型HTTP受控，无付费调用。本轮随机端口临时Java/Python全停，正常8000/8888停，MySQL3306 PID7436保持。

2026-10-19购物车数量修正正式复验：用户已保存WebJsonConfig，107Java package和实际JAR/真实MySQL98项全通过。只操作本轮随机端口Java/受控AI服务，结束全部停止；3306原MySQL7436保留，正常8000/8888停。付费模型调用0，练习cart清理/原五表业务快照一致。

2026-10-19购物车集中验收：107源码package成功；正式JAR/探针均使用自己随机端口，运行结束全部停止。受控AI HTTP无真实模型调用。MySQL7436保持，正常8000/8888仍停；测试cart清除/五表业务快照一致，无DDL授权或正式源码修改。WebJsonConfig修正只在tmp候选class/JAR，正式复验待用户保存。

2026-10-19直接修IDE：当前IDEA2026.2数据源已有shopagent.cart_items元数据；backend/.idea/sqldialects.xml补Mapper文件级MySQL方言与shopagent解析，IDEA原生解析实现验证通过，本地.idea忽略不提交。未改连接、凭据或全局IDE设置。101正式源码构建/6真实Mapper检查通过，测试写入全回滚。助手探针全停；最新3306监听PID7436，正常8000/8888无监听，不复用旧PID7888。

2026-10-19排查IDEA CartItemMapper：正式101源码package通过（未运行项目测试），应用账号实际cart_items/六列可见；SQL写操作未执行，无DDL/权限/源码代改。编辑器仍需单独关联SQL数据源，建议IDEA Database+SQL Resolution Scopes；日志cart-mapping-build.log和cart-ide-datasource-readonly-2026-10-19.json。

2026-10-19购物车只读复查：MySQL8.0.34；cart_items已可见，id/user_id/product_id为bigint、quantity为int、两时间datetime，应用账号SELECT/INSERT/UPDATE/DELETE齐备；证据cart-foundation-readonly-2026-10-19.json。本轮探针0数据库变更且正常退出，未重跑DDL/GRANT，未读取凭据值或新增依赖。当前Java映射/Mapper/输入DTO仅文档待手写，无购物车HTTP验收。

2026-10-19购物车准备：受限应用数据源只读探针确认MySQL8.0.34 Community、app_users/products InnoDB BIGINT主键、cart_items尚不存在，0数据库变更，证据cart-schema-readonly-2026-10-19.json。临时探针自行关闭；012/013迁移脚本仅保存未执行，不读/改凭据或扩大权限；CartItem/Mapper为手写教案，正式未创建。

2026-10-19会话流正式实施：98Java源码package成功，21正式业务+89实际HTTP/真实MySQL检查通过。正常8000/8888起止无监听，助手临时Uvicorn/JAR/受限DB探针全停，MySQL7888/3306保持；未改依赖/权限/真实凭据。仅检查终端Key存在性为false，不读值，无付费模型；实际Python协议解析配受控provider。新验收会话21～26及20条消息77～96保留，其他DB基线不变。后端链已通，真实模型效果与新会话流网页接线待，详CONVERSATION_STREAM_ACCEPTANCE.md。

2026-10-19本轮：正式95Java源码package成功、Python18/Java36/旧流兼容27共81技术检查通过。没有启动正常8000/8888，无数据库操作/真实模型调用/依赖或权限变更；临时loopback探针退出关闭，MySQL7888/3306保持。新AssistantStreamService及AssistantService查询可见性变更仅tmp/assistant-stream-business候选编译21预检，正式源文件未代写。

2026-10-19本轮：正式商品选择通过32 TestClient/受控模型技术检查，无服务启动、付费模型或数据库操作。解释流新教学候选在tmp/explanation-batch编译，Python18/Java36/旧流兼容27检查通过，不替换正式class或应用文件。loopback临时HTTP server随探针退出已关闭；正常8000/8888无监听，MySQL7888/3306保持。无依赖/全局配置变更；新六文件待用户手写。

2026-10-19正式93源码package及模型流转发64技术检查通过，真实临时Java/Python HTTP链为受控provider，实际DeepSeek0。两轮临时JAR/Python/mock服务全停，正常8000/8888起止仍无监听，MySQL7888/3306保留。无依赖/全局配置/正式源码代写或DB写入；Python选商品新接口仅tmp32预检，待用户手写。

2026-10-19事件Codec/线程池正式91源码构建/42检查通过；临时转发候选两JAR/loopback provider均停，原正常8000/8888保持停止，MySQL7888/3306不动。候选仅登录既有fixture读取身份，无DB写入；未装依赖/改全局或正式应用源码，无真实模型或密钥输出。显式关闭候选30通过/旧两参27兼容，正式转发待用户手写。

2026-10-19Java流读取：87源码package及34正式class技术测试通过，含临时实际Python HTTP；临时服务已停，正常8000/8888仍停，MySQL7888/3306保持。无依赖安装/全局环境或DB变更，无真实模型调用；新Codec/线程池仅tmp候选42预检，不写正式业务文件。

2026-10-19Python模型流验收69技术通过，实际临时Uvicorn HTTP结束后已停；当前终端get_api_key配置不可用，真实模型HTTP0（未读取/打印秘密）。正常8000/8888最初及结束无监听，MySQL7888/3306保持。Java教案用Java17和现有RestClient依赖tmp编译/27预检，无依赖/全局配置/DB或正式业务源码改动。

2026-10-19正式锁修正后：85源码package/98针对性检查通过，临时Python/JAR/探针均已停。正常8000/8888原本未运行仍停，MySQL7888/3306保留；专用账号权限未扩大。仅新增3会话/12测试消息；无依赖安装/全局环境变更/秘密读取或真实模型调用。SSE下一课继续现有httpx，不加LangChain等依赖，教案候选26预检仅tmp。

2026-10-19多轮集中验收：正式85源码package成功；实际MySQL8.0.34/专用账号chat_messages仍只有SELECT/INSERT，FOR UPDATE消息查询被拒绝。选择用户改一行FOR SHARE，不执行GRANT或改全局环境。真实临时Python/JAR/DB探针及断连代理已全停；正常8000/8888原本未启动，仍保持停止，MySQL7888/3306不动。无依赖安装/密钥读取/付费请求；D盘验收文档MULTITURN_ACCEPTANCE.md。
2026-10-19Python上下文技术验收：项目.venv导入正确，105检查通过，临时Uvicorn加载正式main/模拟模型后已停止；正常8000和8888本轮开始即未运行，MySQL7888/3306保持，勿沿用前一天9772/7712运行状态。当前终端缺DeepSeek Key配置，真实样例上游HTTP0，未查看/打印密钥或改配置/IDE/依赖；详CONVERSATION_CONTEXT_PYTHON_ACCEPTANCE.md。

2026-10-18发送验收后：79源码package成功，正常Java9772/127.0.0.1:8888替代已核归属停止的旧21392（首次打包受Windows占JAR影响，重试成功）。MySQL7712/3306保持，实时8.0.34/可空recommendation_json已核实；Python8000未启动，临时JAR/模拟上游/Java探针/TCP代理全停。未装依赖/改全局配置/读秘密或收费调用，见CHAT_MESSAGE_SEND_ACCEPTANCE.md。

## 2026-10-17FAISS检索准备与集中检查

项目.venv新增faiss-cpu1.15.1，Windows/Python3.12轮子，版本已固定进requirements-embedding.txt；安装前51个包版本保持，pip check通过。用户正式读取/切分/向量化模块集中运行得到3条512维向量；候选IndexFlatIP检索与3个问题/边界通过，见verification/rag-retrieval-preflight.json。索引为内存，无额外数据库进程/服务端口；尚未代写或验收用户正式knowledge_search.py，无收费模型请求。本条覆盖下文此前“尚未安装FAISS”的旧环境状态，LangChain/LangGraph仍未安装。

## 2026-10-17阶段5本地向量化准备

项目Python3.12专用.venv新增sentence-transformers6.1.0、torch2.14.1+cpu、transformers5.20.1、numpy2.5.4及传递依赖，共33项；安装前18项版本均保持。requirements.txt加引用requirements-embedding.txt，扩展固定新增版本和官方PyTorch CPU索引。pip check及本机已安装环境dry-run通过，未做空白环境重装。

BAAI/bge-small-zh-v1.5固定revision7999e1d3359715c523056ef9478215996d62a620，11文件约96.4MB下载到D:\ShopAgent-X\ai-service\model_cache\bge-small-zh-v1.5。权重SHA校验、Git忽略、CPU离线加载/两段512维向量探针通过；详见verification/embedding-environment.json、embedding-model-manifest.json。无API Key需求、全局环境改动或TLS关闭。下载依赖曾超时一次，增大等待后成功。

用户手写正式embedding_client.py、loader/splitter联通及FAISS/RAG接口仍待后续集中验收；当前未安装FAISS/LangChain/LangGraph，不以本地模型环境就绪当作知识问答已完成。

## 2026-10-17首版网页运行

当前正式JAR含45源码/8静态资源，助手以正常本机外部配置启动在127.0.0.1:8888，页面/shopagent/index.html；用户Python8000保持运行。没有新包安装或全局配置修改。Java进程归属/端口记录在本聊天C盘work/shopagent-web/server.json，停止/重启时确认身份，勿影响其他项目；未来日常服务启动重启/检查用户已授权助手直接处理。浏览器390/320宽度仅视口测试，不代表物理手机能访问回环地址，未公网部署。

## 2026-10-15最新TLS增量

项目HTTPX默认certifi校验官方api.deepseek.com的当前网络证书链失败；使用Python ssl.create_default_context()加载系统默认信任后，无密钥GET /models证书校验成功、HTTP401。实际测试未关闭校验、未读Key或生成模型回答。修复由用户手动修改model_client传入verify上下文；具体证书签发者/网络代理来源未确定，环境变量检测只适用于诊断进程。详见DEEPSEEK_TLS_FIX_STEP.md及verification/deepseek-tls-2026-10-15.json。

## 2026-10-15最新增量：用户授权安装HTTPX

- 检查解释器D:\ShopAgent-X\ai-service\.venv\Scripts\python.exe，安装前HTTPX不存在。
- 已实际安装httpx==0.28.1、httpcore==1.0.9、certifi==2026.7.22；其他原有依赖保持。
- HTTPX导入与版本打印通过，pip check返回No broken requirements found；requirements.txt已由当前项目环境pip freeze写为UTF8，共17项。
- 未改全局Python、业务源码或模型运行配置，未读取密钥/调用模型。以下早期检查记录仅历史。

检查日期：2026-10-11。项目根目录 `D:\ShopAgent-X`。

同日复查：JDK 17.0.20 实际可运行；在单独检查进程中指定这个 JDK 后 Maven 3.9.4 启动成功。MySQL 服务与 3306 监听仍正常，6379 / 8000 / 8888 / 3000 未发现监听，Docker 服务端仍不可连接。Python / pip / Node / npm 命令可运行。未改变全局环境或 IDEA 项目设置，未安装依赖。

开始前的阶段清单见 `START_CHECKLIST.md`：基础工具足够进行第一课，不必把后续功能的所有组件一次准备完。

这里只记录检查时的状态。已安装、正在运行、可登录、能构建是不同结论；本次没有安装依赖、修改全局环境、导入 SQL 或启动参考应用。

## 必需环境

| 环境 | 实际检查结果 | 下一步 |
| --- | --- | --- |
| Git / GitHub | Git 2.54.0；SSH 认证为 wwzyhy，ShopAgent 仓库读取与写协议握手成功；尚未推送 | 本项目 Git 配置与每课提交，正常上传后记录结果 |
| JDK | 默认 Java / javac 25.0.3；另有可用 Microsoft JDK 17.0.20 | 在 IDEA 与本项目终端选 JDK 17，不改其他项目的默认设置 |
| Maven | 3.9.4，当前运行时使用 JDK 25 | 切到本项目 JDK 17 后确认版本并构建 |
| Python | 3.12.10；创建临时虚拟环境和 pip 检查成功 | 自己的 Python 服务使用单独 `.venv`，按阶段固定兼容依赖 |
| Node / npm | Node 24.16.0 / npm 11.13.0 | 可用于前端起步；参考 Vite 5 的实际安装与构建兼容性尚未验证 |
| MySQL | 本机127.0.0.1:3306，8.0.34；原五条虚构商品保留；2026-10-13新增验收练习行6已逻辑删除；006实际授权shopagent_app@127.0.0.1仅products SELECT/INSERT/UPDATE，无物理DELETE；正式CRUD已验收 | 随机项目密码仅存被Git忽略的backend/config/application-local.yml，不进JAR，本轮未读取或重置；root凭据未保存；见PRODUCT_CRUD_ACCEPTANCE.md |
| Redis | 6379 无监听；旧 Windows Redis 3.2.100 的 ping 失败 | 需要可运行的 Redis 7，后续在 Docker 或 WSL 方案中选择 |
| Docker / Compose | Docker CLI 29.7.2、Compose 5.4.0；Docker 服务端连接失败 | 先启动并确认 Docker Desktop 引擎；客户端安装不等于容器可运行 |
| 模型接口 | 检查的进程与用户环境变量中未发现 DOUBAO_API_KEY / DASHSCOPE_API_KEY / OPENAI_API_KEY / COHERE_API_KEY | 之后确定服务商与预算，用户在本机配置专用密钥；本次未读 `.env`，其他文件配置未知 |

Spring Boot 3.2.3 官方文档要求至少 Java 17，明确兼容范围到 Java 21，Maven 至少 3.6.3。当前默认 JDK 25 超出该版本文档的范围；已有 JDK 17 可直接选用，不需要重新下载 Java。

官方依据：https://docs.spring.io/spring-boot/docs/3.2.3/reference/html/getting-started.html

## 已找到的程序位置

- JDK 17：`C:\Users\wzh51\.jdks\ms-17.0.20`。
- Maven：`D:\Maven\apache-maven-3.9.4-bin\apache-maven-3.9.4`。
- Python 3.12：`C:\Program Files\Python312\python.exe`。
- Node：`D:\Node\node.exe`。
- Git：`D:\Git\Git\cmd\git.exe`。
- MySQL：`D:\MySQL\mysql-8.0.34-winx64\mysql-8.0.34-winx64\bin`。
- IDEA 与 PyCharm 目录已找到；VS Code 命令已找到。未确认 IDE 内各项目 SDK 设置。

不要修改 SSH 私钥，也不要把私钥复制到本仓库。项目文档只保存连接方式，不保存密钥内容。

## Python 依赖现状

全局环境已有 FastAPI、uvicorn、LangChain、OpenAI、Pillow、redis、pytest 等，但版本和参考依赖不构成可重复的项目环境。例如全局 LangChain 为 1.3.11，参考要求写成 `langchain>=0.1.11`，并没有固定上界；旧导入路径不能据此保证与最新版本兼容。

检查时缺少的发行包包括 `langchain-experimental`、`faiss-cpu`、`sentence-transformers`、`dashscope`、`pytesseract`、`cohere`、`mysql-connector-python`。不建议一次性全局安装：先建项目虚拟环境，只安装当前阶段需要的包，构建成功后固定版本。

FAISS 本地向量库、云端 embedding / 本地 embedding 是后续 RAG 阶段的选择，不需要现在同时安装 FAISS、Milvus 和所有模型 SDK。

临时探测环境位于 `C:\Users\wzh51\Documents\Codex\2026-10-11\za\work\environment-probe`；它不是自己的正式项目环境。

## 端口与资源

- 3306：已有 MySQL 占用。参考 Compose 将 MySQL 映射到 3306，照搬会冲突；后续可复用本机独立数据库，或将项目容器映射到其他端口，例如 3307。
- 检查时 3000、5173、8000、8080、8888 未发现监听；启动前重新检查。参考 Web 实际端口为 3000，Java 为 8888，Python 为 8000。
- 6379 未发现 Redis 监听。
- 物理内存约 16 GB，检查时可用约 3.7 GiB；C 盘剩余约 62.6 GiB，D 盘剩余约 129.2 GiB。只是当时快照。
- 不同时启动 Android 模拟器、本地大模型和多个大型服务作为第一课要求。云模型方案可以减少本机负担，是否采用仍未决定。

## 可以延后的环境

- Android Studio / SDK：常见安装路径没有找到，ANDROID_HOME / ANDROID_SDK_ROOT 未配置；不代表其他位置一定没有安装。只有开始 Android 阶段才需要进一步检查。
- 参考 Android 配置为 AGP 9.2.1、Kotlin 2.2.10、Gradle 9.4.1、compileSdk 36.1，与部分介绍中的旧版本不同。
- Tesseract：常见安装路径未找到；图片文档 OCR 的可选组件，不是最小 Web 导购的前置条件。
- Milvus / GPU / 本地大模型：当前不作为第一阶段必需项。
- 短信、对象存储：参考实现有这些配置，短信实际为日志模拟；自己的功能与服务方案之后再决定。

## 分阶段准备顺序

1. 确认 JDK 17 的项目配置与 Git 根目录。
2. 创建自己的 Java/Python 最小服务与 Python 虚拟环境；先验证本机 HTTP 通信。
3. 为本项目建立独立 MySQL 库，准备少量可核对的商品样本。
4. 模型调用阶段再配置私有 API 密钥与限额。
5. 会话/认证阶段配置 Redis 7；容器方案先确认 Docker 引擎可用。
6. RAG、Android、多模态按学习进度逐项补齐。

## 检查不覆盖的事项

未验证数据库凭据或建表权限、Maven 完整依赖下载、npm 实际构建、模型账户余额/网络连通性、Redis 7 的运行、Android SDK、全链路功能、任何部署或性能指标。


## 2026-10-12：项目Python环境
已创建ai-service/.venv，Python3.12.10，FastAPI0.143.0/Uvicorn0.54.0实际安装/导入及pip check通过；requirements.txt固定全部依赖，Git忽略venv。正式Pythonmain.py尚未创建，未HTTP启动或模型调用，没有全局pip安装。
# 2026-10-15模型调用依赖增量

2026-10-15后续回退：用户要求恢复10:43步骤起点，以下新增依赖已撤销。项目.venv已卸载HTTPX0.28.1/httpcore1.0.9/certifi2026.7.22，requirements恢复此前14项且与pip freeze一致，pip check通过。模型三个源码模块已移除；以下安装说明仅回退前历史，当前不能按其声称HTTPX可用。见ROLLBACK_2026-10-15_1043.md。

项目ai-service/.venv新增HTTPX0.28.1、httpcore1.0.9、certifi2026.7.22，requirements.txt已固定，pip check无冲突，HTTPX及原FastAPI应用/新增配置模块导入通过。只修改项目虚拟环境，不升级全局Python或pip。服务商/模型/Key未配置或读取，未发模型请求；详见MODEL_CONFIG_STEP.md。

## 2026-10-18注册验收环境增量

实时MySQL8.0.34、CURRENT_USER shopagent_app@127.0.0.1核实；app_users仅SELECT/INSERT，products原权限保持，无广泛写权限。助手未执行GRANT或读取凭据；应用自身加载正常本地配置。用户正式56源码package成功，旧8888 PID16716按归属停止，正常新进程PID14196；原8000 Python PID28116保留。注册临时Spring/JAR/故障服务均停。无包安装/全局配置/模型调用；6测试用户保留并登记，见AUTH_REGISTER_ACCEPTANCE.md。正式发现网站输入类型和事务连接异常映射待修复，不能以环境就绪视为完整注册验收。

