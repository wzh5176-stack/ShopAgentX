# 商品扩展与按型号资料问答

本次直接实现商品资料与RAG优化。新增36款虚构演示商品、36份逐型号说明书和36张本地SVG示意图。耳机8、鞋8、键盘6、鼠标6、背包4、水杯4；每款初始可售库存20、预占0。旧K1资料保留为独立教学样例，不冒充其他商品说明书。

## 本机交付结果

2026-10-25本机MySQL原有82条商品历史记录，实际在售4条；新增后共118条记录、40条在售。下架、逻辑删除和历史测试记录均未重新上架或清理。原商品与原库存行指纹完全一致，其他10张业务表行数未变。新增36条库存记录；第二次导入新增0、保留36，没有重置库存或改价。

商品表及库存表导入前备份位于`D:\ShopAgent-backups\catalog-before-20261025-110815.sql`，13479字节；SHA256：`8a23eee12c9811f55fa786cbb38d35ba4a46d7d87d97eb42a5b0b9182f23f219`。备份在Git仓库外，未提交。

本轮没有新增表或修改历史迁移，不改变订单、付款或会话结构。分类是演示目录元数据；网页分类入口仍使用现有名称/品牌关键词查询，不是新建的商城分类系统。

## 请求实际经过哪里

```text
商品详情 → 查询本款资料 → 资料问答（自动选中商品）
  → POST /api/assistant/knowledge {question, productId}
  → Java KnowledgeController
  → ProductService读取当前在售商品的名称、品牌
  → AiClient发送 {question, productTitle, productBrand}
  → Python按目录精确匹配资料身份
  → 只在当前型号的片段中用BGE/FAISS排名
  → DeepSeek按候选原文回答并返回引用编号
  → Python核对编号与型号，Java复核引用标题
  → 网页显示答案、型号、资料版本、小节及原文
```

价格、库存和购买权限仍由Java/MySQL决定，说明书不作为交易核价来源。Python不直连业务库。

例如E01使用USB-C，不支持无线充电；E02支持USB-C或无线充电。检索E02时，E01片段不会进入候选。选择E02却询问E01，会要求修改问题或切换商品；没有选择商品、问题又没有明确型号时，先请用户说明型号。

## 接口与兼容

Java入口仍为`POST /api/assistant/knowledge`，继承现有权限与CSRF规则：

```json
{"question":"支持无线充电吗？","productId":123}
```

`123`只是示例，必须使用当前数据库返回的真实商品ID。`question`为1–1000个非空白字符，`productId`为可选正整数。指定不存在/不可售商品返回400 INVALID_ARGUMENT；Java不会信任前端提供的品牌、资料路径或商品参数。省略`productId`仍兼容旧调用，但需在问题中写出E02、K1等唯一型号。

Python内部入口`POST /api/knowledge/ask`接受`question`及可选的`productTitle`、`productBrand`；名称和品牌必须同时提供，未知字段或不完整身份返回422。返回结构仍为`data.answer/answerable/sources`，source字段保持`chunkId/source/document/section/content`。资料标题增加版本标记。

资料缺失、型号冲突、不明确或无可用片段时，正常返回`answerable=false,sources=[]`，不请求大模型。检索、模型或引用格式异常仍是受控503，而不是编造成功回答。

## 数据与资料怎么维护

| 文件 | 用途 |
| --- | --- |
| `scripts/build_demo_catalog.py` | 演示数据生成源；写出目录清单、说明书及示意图，不连接数据库 |
| `ai-service/knowledge/catalog.json` | 商品元数据、稳定资料key、精确名称/品牌、版本、别名、主题覆盖及资料路径 |
| `ai-service/knowledge/catalog/*.md` | 逐型号说明书，一份4个小节 |
| `backend/src/main/resources/static/shopagent/catalog/*.svg` | 明确标注虚构演示的商品示意图，无外部图片依赖 |
| `scripts/import_demo_catalog.py` | 独立DML导入工具；默认预览，显式apply才提交 |
| `knowledge_loader/splitter/scope/index/search/service.py` | 加载、分块、限定范围、缓存、向量排序及回答引用校验 |

生成器会重写这些生成文件；以后扩展请修改生成源并升级VERSION，再重新生成。不要一边只改生成文件，一边又运行生成器覆盖自己的修改。

导入开发依赖：

```text
python -m pip install -r scripts/requirements.txt
python scripts/import_demo_catalog.py --local-config
python scripts/import_demo_catalog.py --local-config --apply
```

应从仓库根目录使用已安装依赖的解释器执行。`--local-config`只程序消费本项目忽略的本机数据源密码，不打印密码；也支持通过`--password-env`指定环境变量名。不要把密码写在命令参数里。目标限于`shopagent`或隔离测试库`shopagent_regression`。

在其他环境导入前先备份；有已有商品而缺库存、名称品牌重复或其他错误时，整批回滚。工具用数据库命名锁串行导入，再在一个事务里新增商品及库存。已存在的同名同品牌行全部保留，不覆盖描述、价格、上下架或库存。它不是更新商品资料/恢复库存的脚本。

跨环境商品ID可能不同，所以说明书key不写死数据库ID。当前采用名称+品牌精确绑定，改名后会安全提示缺资料；如将来需要频繁改名，应增加正式资料绑定字段与迁移，不能依赖模糊匹配悄悄换成另一型号。

## 向量缓存与边界

37份资料切成147个片段，使用本地BGE-small-zh-v1.5的512维归一化向量，在型号范围内用FAISS内积排名，最多3段。不是全库先取3段再过滤。加载模型仍使用`local_files_only=True`。

缓存键包括资料内容、版本、名称、品牌、别名、主题和固定模型/分块约定版本。改变资料后，下次检索自动重建；更换模型/归一化/分块约定时需要升级`knowledge_index.py`中的版本标识。`vector_store/knowledge-<hash>.npz`被Git忽略，读取禁止pickle，并检查片段、维度、有限值和归一化。缓存损坏会重建，容器目录不可写时退回内存，不要求另装向量数据库。

主题覆盖表是保守的关键词门槛，不是通用的语义相关性分类器；同义表达可能被拒绝。当前未标定余弦阈值。来源/型号验证只能阻止无效引用和串型号，不能数学保证大模型每句话都与原文一致；真实模型生成质量需要独立评测。

## 本轮验收证据

- 集中回归114项全部通过：Java单元61、真实隔离MySQL业务7、Python36、工程8、导入MySQL2。见`tmp/regression/20261025T031638715553Z/summary.json`。
- 真实本地BGE/FAISS评测72个有资料问题：预期小节Recall@3为72/72，Top1为71/72；跨型号候选0。42个不明确/冲突/资料不足检查通过。见`tmp/catalog-rag/retrieval.json`。问题由演示目录覆盖构造，不是通用用户问题分布，也不能写成大模型答案100%准确。
- 向量缓存5项独立测试通过：内存/磁盘复用、改版重建、损坏恢复、异常空间拒绝、只读目录降级。
- Edge真实页面在390px和1440px通过筛选分页、SVG加载、详情跳转、商品ID请求、CSRF请求流程、原文转义、切换清旧引用和无依据状态检查。使用模拟API，未冒充运行中整站验收。截图与报告在`tmp/catalog-rag/ui/`。
- 本机导入及保留证据：`tmp/catalog-rag/local-import.json`，新增36款各字段与720可售库存另见`product-verification.json`。最终汇总`tmp/catalog-rag/acceptance.json`；新JAR内36张图和新版脚本已核对。

复跑真实检索/缓存（需完整RAG依赖和已下载本地模型）：

```text
python scripts/evaluate_catalog_rag.py
python scripts/check_rag_cache.py
```

浏览器回归使用`node scripts/check_catalog_ui.cjs`；需要Playwright及浏览器，可用`SHOPAGENT_PLAYWRIGHT_MODULE`、`SHOPAGENT_EDGE_PATH`指定本机位置。它不启动正式服务、不产生订单或付费模型调用。

本轮DeepSeek真实调用0次，没有读取IDE密钥、创建支付或重启业务服务；GitHub Actions未远端运行，GitHub与VMware部署尚未同步。新版代码和数据目前位于Windows项目与本机数据库，Linux旧运行环境不会自动变化。

后续发布更新：本轮商品/RAG与自动化整理已随库存闭环部署Linux，且已补真实DeepSeek对应型号引用联调；此前“未部署/0次模型调用”描述仅是该轮阶段边界。最新状态见[库存闭环](INVENTORY_CLOSURE.md)。
