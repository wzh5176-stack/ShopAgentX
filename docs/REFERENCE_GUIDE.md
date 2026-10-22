# ShopAgent-X 参考项目阅读笔记

日期：2026-10-11。参考仓库：https://github.com/EvanYao826/ShopAgent-X

固定提交：`e45e59402206beb262d0fc8d007ee9b7a232b644`。以下属于该提交的静态源码阅读，不是本机运行验收；阅读范围见 `READING_COVERAGE.md`。

## 1. 项目究竟在做什么

这是一个商品业务 + AI 导购/知识问答项目。Java 管账号、商品、购物车、会话和业务数据；Python 管模型调用、检索与 Agent；React 和 Android 提供用户界面。

自己的学习项目要逐步形成这样的可验证闭环：用户提出需求 → 查真实商品 → 用模型组织有依据的推荐 → 展示真实商品卡片 → 用户确认购物车操作 → 记录过程和结果。

参考仓库的功能文件很丰富，但不能据此认定交易、权限、部署或性能已经完整可靠。自己的简历要围绕亲自实现并验收的闭环。

## 2. 模块地图

| 目录 | 职责 | 阅读入口 |
| --- | --- | --- |
| `backend/src/main/java/com/demo/aiknowledge/` | Spring Boot 业务服务，Security/JWT、MyBatis-Plus、缓存、商品、购物车、会话、订单、后台 | `controller/ChatController.java`、`service/impl/ChatServiceImpl.java`、`AiServiceImpl.java` |
| `backend/python-service/api/` | FastAPI 对话、文档处理、多模态和 Agent 接口 | `routes.py`、`agent_routes.py` |
| `backend/python-service/workflows/` | 意图分发和不同业务 Agent | `router_agent.py`、`shopping_agent.py`、`knowledge_qa_agent.py`、`reasoning_agent.py` |
| `backend/python-service/agent/` | 自定义状态、计划、执行、事件和策略 | `orchestrator.py`、`planner.py`、`executor.py`、`state.py`、`policies.py` |
| `backend/python-service/tools/` | 工具元数据、注册、调用跟踪与具体工具 | `__init__.py`、`base.py`、`registry.py`、`cart_tool.py` |
| `backend/python-service/core/` | 配置、模型、向量库、MySQL、Redis、文档解析切分 | `config.py`、`llm.py`、`vector_store.py`、`parser.py` |
| `backend/frontend/` | React 页面和管理界面 | `src/App.jsx`、`src/api/index.js`、`vite.config.js` |
| `android/` | Kotlin / Compose Android 客户端 | Gradle 配置、导航、ViewModel、网络 API、SSE 处理 |
| `backend/sql/` | 主建表、订单建表和补丁/种子数据 | `init.sql`、`order_tables.sql` |
| `data/` | 100 个商品 JSON 样本、资料和导入脚本 | `import_products.py`、`ecommerce_agent_dataset/` |

## 3. 一次实际请求怎么走

### 普通对话

1. Web 的 `chatAPI.sendMessage` 发给 Java `POST /api/chat/messages`。
2. Java 从认证上下文取得用户，读取/持久化会话与消息，准备上下文/画像。
3. Java 的 AI Service 请求 Python `POST /api/ask`；配置中的 AI 服务基址带 `/api`。
4. Python 的 `ChatRequest` 使用 `question` 字段，另有 `conversation_id`、`user_id`、`context`、`username`、`gender`、`skin_type`、`preference_tags`、`jwt_token`、`is_admin`；画像文本在内部组装，不能把内部 `user_profile` 当作请求字段。
5. Router 调用意图分类，根据任务交给导购、购物车、问答、闲聊或后台等 Agent。
6. Python 返回 `answer`、`sources`、`task_type`，部分业务还有 `product_cards`、`confirm_card`、`cart_selection`。
7. Java 保存结果并返回前端。

### 流式对话

Web `POST /api/chat/stream/messages` → Java `SseEmitter + WebClient` → Python `POST /api/ask/stream` → 路由与业务流 → JSON 事件 → Java 转发 → 页面逐步展示。

流式接口与普通接口分开实现，不能默认行为一致。购物车 `list` 的流式结果有 `cart_list` 事件；普通接口使用 `cart_selection` 承载类似数据。

### 购物车形成的反向调用

Python ShoppingAgent → 工具名 `cart_operation` → 带 JWT 的 HTTP 请求 → Java `/api/cart/...` → Java 根据认证身份改自己的业务数据。

这个项目确实存在 Java 与 Python 的双向服务协作。用户身份和权限不能只相信请求中的 `user_id` / `is_admin`。

## 4. 导购：数据库和 RAG 各负责什么

- `shopping_agent.py` 的推荐读取向量检索得到的知识文档，同时通过 `_search_products` 查 MySQL 商品。
- 商品搜索包含模型提取关键词/排除词、正则回退、品牌品类匹配、同义词展开、参数化 LIKE 查询、类别筛选与后处理。
- 商品卡片由数据库候选构建；向量检索主要提供回答依据。不要笼统说所有商品都来自“向量 + SQL 合并排序”。
- `_post_process_products` 实际综合分为：相关性 0.5、评分 0.2、销量 0.3（归一化后）；注释仍写评分 60% + 销量 40%，应以实际代码为准。
- 非流式话术生成有价格文本校验；流式调用另一条模型链，不能直接推定复用了全部非流式校验。
- Java 的卡片校验会过滤不存在/下架商品，并修正部分字段；流式卡片先转发，完成时才校验持久化结果。需要验证显示内容与保存内容是否一致。

自己实现时先用 5～10 个可人工核对的商品。至少验证：明确品类、预算、品牌排除、无匹配、下架、价格准确、跨轮指代、模型失败。

## 5. 知识问答、规划与工具

### 知识问答

`KnowledgeQAAgent.ask` 的 L1 实际为向量检索 + 模型回答，并不是无模型的商品字段直接返回；L2 使用 RetrievalAgent；复杂问题的普通路由会进入 ReasoningAgent。

ReasoningAgent 用模型拆成子问题，逐个检索和回答，最后合成。自定义 Orchestrator 另有计划/执行路径；不是每一个问题都通过同一个通用 Agent 循环。

RetrievalAgent 支持改写、召回、重排和引用，但默认 `use_rewrite=False`、`use_rerank=False`。模块存在不等于所有请求都启用这些能力。

### 规划与策略

Planner 有关键词分类、固定步骤列表和启发式充分性判断。需区分规则规划、模型子问题分解与通用自主规划。

Policies 包含重试、输入检查、超时/步数等配置；部分辅助方法存在不等于全链路执行生效。ToolRegistry 的超时等待与 `future.cancel()` 不能保证已经运行的线程终止。

### 工具注册

启动注册代码列出 8 个工具，而非文档中的 7 个。实际工具名：

| 工具名 | 作用 |
| --- | --- |
| `question_rewrite` | 改写问题 |
| `knowledge_search` | 检索知识 |
| `rerank` | 重排候选 |
| `conversation_memory_read` | 读取会话上下文/摘要 |
| `conversation_memory_write` | 写入会话消息 |
| `doc_summary` | 文档摘要 |
| `ocr_extract` | 图片文字提取 |
| `cart_operation` | 调 Java 购物车 |

这里是静态注册清单，尚未证明本机每个工具成功初始化/执行。部分购物车路径直接调用 `tool.execute`，绕过 Registry 的统一调用跟踪与策略；权限字段主要是描述，仍要检查实际校验点。

## 6. 记忆机制

- Redis 存消息列表，消息 TTL 24 小时，摘要 TTL 1 小时。
- 压缩条件按消息列表长度超过 10，保留最近 5 条消息；注释用“轮”，代码实际是“条”，两者不同。
- `memory_read.py` 的摘要路径调用 `LLMService.chat`，当前 `llm.py` 没有这个方法，异常会走回退；后续要验证真实摘要行为。
- MemoryAgent 的用户画像提取也调用这个缺失方法；`UserMemoryClient` 查询 `user_memory`，仓库 SQL 未找到对应建表。用户长期记忆能力需单独核实。
- ShoppingAgent 的最近推荐、待确认操作和购物车缓存用进程内字典，购物车缓存判断 30 秒 TTL。进程重启/多 worker 时的状态一致性需要另做设计与验证。
- 这是参考应用自身的记忆，与 Codex 聊天压缩不是同一个机制。

## 7. 数据库与部署差异

1. `init.sql` 实际 23 张表；`order_tables.sql` 另有 `order` / `order_item` 两张。Java 还有 Address 实体，Python 查询 `user_memory`，但仓库 SQL 未找到这两张表的建表。
2. 根 Compose 只挂载 `init.sql`，不会自动运行订单脚本和商品导入。因此照快速开始不等于获得全部订单表与商品数据。
3. 仓库有 100 个商品 JSON；`data/import_products.py` 负责导入，它依赖 `pymysql` 并有硬编码本机数据库示例配置，不能照搬真实凭据。初始 SQL 没有自动插入 100 个商品。
4. SQL 包含 DROP TABLE；本次没有执行。自己的项目用独立数据库和可追踪迁移。
5. Java 默认 8888，Python 8000，Web Vite 配置实际 3000。Android 模拟器地址 `10.0.2.2:8888` 是访问宿主机的约定，不应照搬到真机。
6. Python 默认 `LLM_PROVIDER=dashscope`、embedding 为 dashscope。`USE_MILVUS` 默认 true，但缺少 pymilvus 时可回退 FAISS；需要显式确定自己的配置。
7. cloud embedding 缺少密钥时可能回退本地 BAAI 模型并下载。第一课不通过导入参考应用触发未知下载或数据库副作用。
8. 前端使用 React 18、Vite 5、Axios、ECharts、React Router；package.json 没有 Ant Design，不能采用前端 README 的推测。
9. Android 实际使用 Koin，Gradle/AGP/SDK 配置与旧文档不同；签名包含作者本机路径和示例口令，不复制到自己的项目。
10. 登录短信实现当前是日志模拟，订单支付是状态更新；不能称为已接入真实短信或支付渠道。

## 8. 需要后续验证或修复的具体入口

| 事项 | 静态证据/推断 | 后续验证 |
| --- | --- | --- |
| Web SSE 解析 | Java 转发具名 event；Web 仅处理整个分段以 `data: ` 开头的内容，存在协议不匹配风险 | 拿实际 SSE 字节检查具名事件、多行 data、断包、换行 |
| 检索分数 | `vector_store.search` 将 score >= threshold 视作高相似 | 核对选定 FAISS/Milvus 的距离定义、归一化、过滤方向，人工标注检索样本 |
| 搜索 SQL 参数 | 多关键词循环中参数交错追加，而 SELECT 与 WHERE 的占位符分组排列 | 用两个以上不同关键词验证每个参数对应位置与结果 |
| 记忆摘要 | `LLMService.chat` 未定义 | 超过阈值后检查真实摘要与失败降级 |
| 普通模型回答 | 共用 Prompt 包含 season，普通 `get_answer` 的 invoke 参数未传 season；流式传入了 | 实际普通问答不能只测流式路径，检查缺参数错误与回退内容 |
| 工具结果协议 | KnowledgeSearchTool 返回文档 dict，Executor 部分路径按对象属性访问 | 检查内容/metadata/引用是否传到回答，而不是 dict 字符串或空字段 |
| 购物车数量 | CartTool add 读取 quantity，但发出的请求没有携带数量 | “加 3 件”验收数据库实际数量 |
| 普通加购确认 | 非流式 `ADD_CONFIRM:` 分支调用未找到定义的 `_generate_add_confirm_response` | 对比普通/流式协议，覆盖客户端确认分支 |
| 状态与权限 | 待确认按 conversation_id 索引；会话与后台若干方法的所有者/角色校验需要贯穿检查 | 两个普通用户隔离、伪造 ID、普通用户访问后台、过期确认 |
| 订单 | SKU 归属、库存/下架校验与并发状态转换尚需完善；支付当前为状态更新 | 不同商品 SKU、库存不足、重复支付/取消、并发请求 |
| 运行记录 | 部分结束路径可能按 success 记录；token 与 answer 的汇总语义不同 | 模型失败和断流不能计成功；结果保存内容核对 |
| Agent 中断 | Orchestrator 的 get_state / interrupt 返回占位值 | 不宣传已支持运行恢复/有效中断 |

这是一份未来验收入口清单，没有通过运行复现的风险不要写成已证实线上漏洞，也不代表要在第一课把所有问题同时修完。

## 9. 文档掌握与自己的简历

7 份文档中的架构、学习顺序、部署、排错、设计取舍与面试题已读，但它们不是自己能力和性能的证据。自己的学习档案每阶段保存：实现 → 原因 → 验证 → 实际结果 → 提交。

最初可以解释“Java 为什么管业务和权限、Python 为什么管 AI、请求如何跨服务、商品价格为什么从库里取”；后续再加入检索评估、工具安全、失败处理与性能数据。

参考 LICENSE 是 MIT，版权署名 EvanYao826；README 另有学习/研究限定文字。仓库表述存在差异，复用具体代码应保留对应许可与来源，发布或商业使用前再核对适用范围。当前只阅读，没有复制参考应用代码到自己的仓库。

## 10. 聊天记忆怎么保存

Codex 的聊天压缩会保留关键状态，但不保证每个旧细节原样可用。自己的代码与文档才是可核对的恢复入口。

- `AGENTS.md`：要求每次恢复先读状态与当前模块。
- `PROJECT_STATE.md`：长期目标、用户基础、完成/待做、环境、下一步。
- `LEARNING_LOG.md`：每课成果与决定。
- `ENVIRONMENT.md`：实际环境与配置缺口。
- `READING_COVERAGE.md`：哪些资料已完整阅读、哪些源码还待逐行读。

以后在 Codex 以 `D:\ShopAgent-X` 作为项目目录开展工作，使根目录约定可被发现。本聊天目前仍在单独的 Documents/Codex 工作目录，不能声称 D 盘的说明已经自动进入所有新聊天；本次已明确读取并使用。项目文档没有修改全局个人记忆。

官方说明：[AGENTS.md](https://learn.chatgpt.com/docs/agent-configuration/agents-md)、[Codex compaction](https://developers.openai.com/cookbook/examples/gpt-5/codex_prompting_guide)。
