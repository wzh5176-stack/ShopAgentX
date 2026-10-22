# ShopAgent：按实际开发推进的教学路线

2026-10-22首版收尾：独立Cluster/SQL恢复实验已验并停止，商品基线用户实际30串行全成功、mean9.89/P9514.94ms（非容量上限），README、实测报告、演示复盘、贡献简历草稿和结构化快照已归档，39内链检查通过。当前约定的最小可演示版本开发与本机验收/说明已完成；下一按DEMO_AND_REVIEW.md复盘核心请求路径及Cookie/CSRF旧暂缓项，不默认添加业务。没有公网生产发布、自动新机初始化或版本化回退包；LangGraph/交易闭环不在已实现清单。

2026-10-22阶段8更新：当前Linux备份已手动隔离恢复，助手只读核六表及6/11/31/167/1/35行、结构约束、11关联0异常；见MYSQL_RESTORE_CHECK_ACCEPTANCE.md。Cluster已停止，下一停止恢复库→少量不调用模型的实测指标→README/演示/贡献交付；未逐行比SQL或恢复应用切换，不把教学实测包装为生产容量。

2026-10-22阶段8更新：独立RedisCluster六节点组建、三分片练习写读、主从复制、停主提升、恢复原主为副本并同步新值已完成手动演练和只读核实（REDIS_CLUSTER_ACCEPTANCE.md及verification快照）。下一实验键清理/停止→现状SQL隔离恢复→实际指标和README演示贡献。未测故障窗口、非跨机容灾、不称网站使用Cluster；不再追加新业务。

2026-10-22阶段8更新：Linux完整六服务healthy/入口UP由助手只读确认；真实多轮流式、历史、资料引用、购物车确认恢复和停A后本人/业务正常由用户反馈通过，助手另核A恢复healthy。运行手册RUNTIME_RUNBOOK.md已保存。下一当前Linux库手动备份并下载核SHA；之后独立RedisCluster、实测指标和README/贡献台账。单VM故障演练不称生产HA；LangGraph、订单支付未实现，当前新备份/恢复和Cluster未执行。

2026-10-21部署启动进展：用户已手动启动Linux六服务，助手独立只读确认全healthy、Nginx8080对双Java分发200/首页商品HTTP正常。下一RUNTIME_WEB_MANUAL_STEP.md从Windows仅正向网页转发验证普通登录/真实多轮流式/历史恢复/资料引用，再购物车确认与停实例/收尾。不是生产高可用或全部业务已验，Cluster仍独立后续。资源快照Linux4GB/轻载用约1.7Gi，未压测。

2026-10-21镜像准备进展：Java/Python dev镜像均已SSH只读确认存在；不是应用容器启动或HTTP验收。当前EMBEDDING_MODEL_MOUNT_MANUAL_STEP.md用户手动复制BGE模型、只读挂载并离线运行原检索脚本。完整LinuxCompose前先评估VM约2GB内存资源，再数据库备份/独立演示库/私密外部配置。Nginx故障与代理SSE仍未获明确结果，后续集群/最终交付保持。

2026-10-21阶段8进展：Redis与双Java已完成环境检查；用户报告Nginx手动启动组通过，助手独立只读六次8080健康均200/UP且A/B交替。下一NGINX_FAILOVER_MANUAL_STEP.md在统一入口手动停A/恢复、身份与退出；完整Cookie/CSRF/故障/SSE集中验收仍未完。本地开发采用Linux Nginx+显式SSH正反向转发接Windows IDEA Java，保持回环和现有网络设置。此为开发接线，不替代后续完整Linux容器化部署；RedisCluster/性能测量/最终交付仍待。

2026-10-20阶段8最新：VMware Linux Redis环境已就绪，采用Linux Docker Engine+原Compose回环发布+SSH转发至Windows127.0.0.1:6379，避免开启Windows Hypervisor影响eNSP；参见REDIS_VMWARE_ENVIRONMENT_STEP.md。实际PONG/临时写读TTL清理/容器healthy等环境检查通过，未构建Java或验收共享会话。下一仍集中验收用户已写三处配置与Spring Session单实例，再两Java/Nginx与RedisCluster；本次只调整本地运行方式，不更改业务范围。完整部署/指标/简历验收未完成，eNSP与VMware客户机继续轮流运行。

2026-10-20阶段8第一组手写课SESSION_REDIS_MANUAL_STEPS.md：专用Compose、Boot4 Session Redis starter、公开application.yml三处完整候选及解析，保留JSESSIONID；正式用户手写待验。已请求启动Docker Desktop，进程出现但引擎info超时，未建Redis/运行构建/共享会话验收。三处完成后先专用Redis健康，再正式编译/单实例Cookie-CSRF-退出及跨Java和故障集中检查。阶段8仍准备中，Nginx/RedisCluster后续，未代写后端。

2026-10-20阶段7收尾已做真实模型演示：新资料问答网页+引用入口，原密钥Python实际运行；真实RAG/两轮导购/购物车待确认→人工确认→恢复→独立cart→移除27项检查通过，最终CSS后25受控UI+4只读复核通过。见KNOWLEDGE_WEB_AND_REAL_DEMO_ACCEPTANCE.md。阶段5/6/7的最小网页链可演示，不当LangGraph图编排/完整订单完成。下一阶段8先Redis单节点共享登录会话再Nginx双Java，RedisCluster最后独立演练；Docker CLI存在但引擎未运行，本轮只有环境只读检查，无部署或后端代写。

2026-10-20用户纠偏后的商城范围：此前AI工作台不等于完整商城前端。本轮已补独立home/products/product/cart页，默认入口转home，保留AI/原版/其他设计。正式131源码构建、商城48技术检查及CSS收尾后最终18只读布局/身份检查通过，见STOREFRONT_ACCEPTANCE.md。基础选购路径已接真实商品和cart；订单支付/地址物流仍未实现，不因页面齐全称交易平台完成。下一真实AI完整演示与复现，订单范围独立定义，阶段8部署扩展不提前。

2026-10-20阶段7前端技术接线已验收：选定跳糖选物接本人会话、导购流、turns待确认卡片与普通购物车，正式打包JAR/实际Edge等36检查通过；见WEB_WORKSPACE_ACCEPTANCE.md。原页面和其他风格保留。真实供应商语义/最后完整演示待原PyCharm密钥环境，不把受控模型结果当真实效果；未实现LangGraph图编排。正常8888已启动、8000留给原配置；下一先真实演示和复现说明，不提前Nginx/Redis扩展。

2026-10-19阶段7最新：工具历史三组正式131源码构建、226技术检查通过（19结构/85无外层测试事务故障回滚/122实际HTTP并发回归及收尾），见TOOL_MESSAGE_ACCEPTANCE.md。后端工具消息/操作同事务、版本409和历史恢复协议已接通；下一前端按既定助手实现接选定跳糖选物的私有会话、流/工具确认卡片及购物车，再真实模型/最终复现。真实语义和浏览器未验，不当阶段7或LangGraph已全完成，不提前部署扩展。

2026-10-19阶段7当前TOOL_MESSAGE_WRITE_MANUAL_STEPS.md：三文件手写完整组——响应增加工具已存消息、独立工具短事务写入服务、分流模型前捕获版本及提交接线。未代写/运行。下一优先016/历史DTO/本组整体正式构建、真实MySQL HTTP和回滚并发验收，不新增业务；通过再页面卡片恢复与真实模型演示。

2026-10-19阶段7当前TOOL_MESSAGE_RESPONSE_MANUAL_STEPS.md：三文件完整增量手写迁移MessageResponse七字段及历史/保存三处构造，不用兼容重载掩盖漏改；未运行本组。下一工具消息与操作同短事务保存/会话版本保护和分流接线，整组验收后页面恢复；016实际执行仍待集中核实。

2026-10-19阶段7当前TOOL_MESSAGE_STORAGE_MANUAL_STEPS.md：给016消息类型+操作引用迁移与ChatMessage实体完整手写组，未代写正式文件/未执行DB验收。下一消息DTO/历史映射→工具消息与操作同短事务、expectedLastMessageId冲突保护→正式整组验收→页面恢复卡片。保留分流128通过，普通澄清为TEXT；工具状态读取原操作表不修改历史文本，权限继续SELECT/INSERT。

2026-10-19最新阶段7里程碑：会话分流130源码正式构建、128项实际JAR/MySQL/Python技术检查通过，见CONVERSATION_TURN_ACCEPTANCE.md；工具待确认后端及新非流式turns已接通，真实语义/页面未验。下一工具消息保存与operationId关联恢复、会话版本和短事务原子边界，再前端卡片/完整复现，不跳阶段8。首版约80%仅范围估计，不按测试数量上调。

2026-10-19阶段7当前CONVERSATION_TURN_HTTP_MANUAL_STEPS.md：上组三DTO/Service文件存在但未运行验收；本组Controller+精确POST turns USER规则/请求解释供用户手写，未代写应用。下一优先上组+本组集中验收，之后工具历史/卡片恢复/会话版本和页面；不称普通messages/stream已接工具，不跳部署扩展。

2026-10-19阶段7当前课CONVERSATION_TURN_ROUTING_MANUAL_STEPS.md：三文件手写分流DTO/Service，复用原导购和prepare/create；无来源旧导购，有来源none/clarify/set_quantity分路，明确目标只生成待确认单。正式应用未写/未验，不称HTTP或页面已接。下一Controller+精确安全规则后集中验收；再工具消息保存/卡片恢复与会话冲突衔接及页面，不提前部署扩展。首版约80%仅主观范围估计，非检查统计。

2026-10-19最新里程碑：阶段7购物车操作单后端整组正式验收通过：126Java源码构建、154项技术检查（135主验收+19已有操作独立事务回滚补验）。证据见CART_ACTION_ACCEPTANCE.md及两份cart-action报告；不是完整Agent或真实模型语义/网页验收。下一先会话工具分流与人工确认卡片接线，保持Java/Python手写、分组验收；之后最终复现，阶段8扩展顺序不变。下方早期手写候选状态由本条和阶段表覆盖。

2026-10-19阶段7当前CART_ACTION_DECISION_MANUAL_STEPS.md：确认/取消/幂等3文件完整增量手写课；当前操作源码准备读取不当已通过。下轮整组正式构建/真实MySQL与HTTP（含表权限/创建读取/终态/并发/过期/基线与回滚），通过再页面与真实模型演示收尾。

2026-10-19阶段7当前CART_ACTION_CREATE_READ_MANUAL_STEPS.md：操作创建/本人读取ServiceController+Clock/404/精确USER规则完整手写组；用户称已运行建表授权，本课不逐步验收。创建只保存待确认单不改cart，GET只读过期投影；下一确认取消与原子幂等再整组集中验证。

2026-10-19阶段7当前CART_ACTION_STORAGE_MANUAL_STEPS.md：cart_actions表/权限/实体Mapper四步用户手写供后续计划创建确认使用，本课未执行/验收。按用户既有排除Nginx/Redis集群扩展的首版演示范围，主观进度75%～80%；剩确认取消/幂等、必要页面接线、真实模型和演示复现收尾，不按检查数量自动涨进度。

2026-10-19阶段7当前Python+Java工具意图/prepare接线正式通过：文件名拼写修复后120源码package、正式Python51+实际Java/真实DB/真实Python受控上游102=153技术检查全通过。无Java代写或提交DB变更，真实模型语义/前端未验。下一Java操作计划表和PENDING保存/状态读取、确认取消、状态与cart原子幂等，按整组手写和集中验收推进。见CART_TOOL_WIRING_ACCEPTANCE.md。

2026-10-19用户要求合并Java整组再验收：当前JAVA_CART_TOOL_WIRING_MANUAL_STEPS.md完整Java消费Python/真实推荐位置复核/prepare HTTP，10新类+3处增量供手写，无本轮验收。等整组写完合并Python/Java集中检查；prepare返回识别结果，持久化操作计划/确认取消与原子幂等执行后续独立业务组，不能称Agent全程完成。

2026-10-19阶段7当前CART_TOOL_INTENT_MANUAL_STEPS.md：合并Python工具意图schema/service/HTTP三步骤供用户手写，输出仅商品顺序位置与目标总量/clarify/none，不执行购物车。候选51技术预检非正式实现/真实语义。下一本组正式验收→Java消费并按同一来源推荐映射真实商品→计划持久化和用户确认/取消、幂等执行。

2026-10-19阶段7：普通购物车修正已正式107源码package/98项全通过。当前CART_ACTION_DTO_MANUAL_STEPS.md用户手写3文件定义操作请求/响应/状态，先协议，不宣称Agent已接线；再Python工具意图→Java复核真实推荐/本人会话并持久化计划→确认取消及原子幂等执行。已有前端暂停和真实模型效果待验边界保持。

2026-10-19阶段7购物车已做正式集中验收：107源码构建成功，98项96通过，2项数量格式自动转换待修。当前CART_RECAP_AND_INPUT_FIX_MANUAL_STEP.md用户手写WebJsonConfig整数严格转换，隔离候选98/98不当正式通过。先正式复验，再Agent计划/确认/取消/幂等，不因教案或候选通过提前推进。证据CART_ACCEPTANCE.md。

2026-10-19阶段7当前CART_CONTROLLER_MANUAL_STEPS.md：普通购物车最后接线课（写响应DTO/Controller三路由/精确USER规则保留CSRF），用户手写待验收。PUT200只回商品编号与绝对数量，GET整车汇总，DELETE204无正文。下一优先整组正式构建与真实DB/HTTP验收，不新增Agent直到基础通过；本课无应用代写或运行。

2026-10-19阶段7当前课CART_READ_MANUAL_STEPS.md：购物车两响应DTO+Service只读列表/金额，批量查商品避免逐行查询，保留下架和逻辑删除条目供移除；所有条目总件数、仅可用金额分开。仅教学候选编译，无正式运行。下一Controller/安全路由和写响应，再务必整组正式构建/真实DB和HTTP验收；工具计划仍后续。

2026-10-19阶段7当前课CART_SERVICE_MANUAL_STEPS.md：设置数量/本人移除Service+商品不可用异常和Advice409，复用认证与Mapper，事务内商品共享锁和购物车当前回读；用户手写，tmp3单元编译不等于正式运行。下一查询响应与Controller后集中真实DB验收，再工具计划/确认。

2026-10-19阶段7当前：数据库cart_items列与应用四权限已只读确认，012/013无需重跑；整体约束与行为还待集中验收。CART_MAPPING_AND_REQUEST_MANUAL_STEPS.md给三文件手写：实体、含原子目标数量upsert的Mapper、数量请求DTO；不代写正式源码或据教案记完成。下一CartService→查询响应/Controller与安全和真实DB验收→工具计划确认。

2026-10-19阶段7开始准备：CART_FOUNDATION_MANUAL_STEPS.md，先本人购物车普通业务再Agent计划/确认/幂等执行。已只读核实8.0.34与cart_items不存在，012建表/013仅新表授权脚本已保存但未执行，用户手写CartItem/Mapper待实施；不计为购物车功能完成。下一DTO/目标总数量upsert/Service→查询DTO/Controller与真实隔离/重复请求/失效商品验收，再工具确认流程。旧阶段6代写范围不自动扩大到本阶段，继续完整代码教学/成组检查。

2026-10-19当前：用户授权直接实现本轮会话流接线；正式98源码package/商品流业务21+实际HTTP和真实MySQL89技术检查通过，见CONVERSATION_STREAM_ACCEPTANCE.md。阶段6后端多轮/流式/成对保存已接通，done晚于短事务提交；已测试归属/CSRF/异常/并发版本/有界队列/取消提交边界。真实DeepSeek效果仍待（终端无Key），新会话流前端未接线，前端学习暂停。下一主线阶段7 Agent/购物车最小场景；必要页面接线和真实模型复验保留，阶段8Nginx/Redis集群不提前堆入。进度以实际交付范围说明，约70%仍为旧主观估计，不按测试数量上调。

2026-10-19当前：正式推荐解释组95源码package/81技术检查通过（Python18/Java36/旧流兼容27），见RECOMMENDATION_EXPLANATION_ACCEPTANCE.md，不当真实模型与会话落库链已完成。当前ASSISTANT_STREAM_BUSINESS_MANUAL_STEPS.md两文件用户手写：开放已有条件候选查询；新商品流业务串编号映射/真实事实解释/完整校验后返回结果。tmp候选21通过不当正式实施；写完先集中验收，再会话流Service/Controller和保存冲突取消提交边界。按首个可演示功能版、不计Nginx/Redis集群，主观粗估约70%；以已交付范围说明，不能以检查数量上调。Agent/购物车、必要页面接线和最终复现验收仍在主线。

2026-10-19当前入口：正式商品选择Python32技术检查通过，见PRODUCT_SELECTION_ACCEPTANCE.md。按用户一次多教几步给RECOMMENDATION_EXPLANATION_MANUAL_STEPS.md三组六文件：Python已选商品解释流+Java选择DTO/两业务客户端调用及共享流读取。候选18+36+27检查通过不等于正式实施；手写完本组先集中验收，再私有多轮流业务编排、真实商品映射、完整校验与版本保护短事务保存。旧单轮及非流式接口保留，前端继续暂停，3模型步骤与真实语义待验边界不变。

2026-10-19最新：正式93Java源码package/管理员模型流转发64技术检查通过（MVC/安全/饱和/超时显式关闭30+旧AiClient27+正式Java→实际Python HTTP→模拟provider7），详JAVA_MODEL_STREAM_RELAY_ACCEPTANCE.md及verification/java-stream-relay-formal-2026-10-19.json/chain报告。原样正式代码验收，不用候选class替代；无真实模型/DB写入或源码代改，临时全停，正常8000/8888仍停，MySQL7888保持。当前手写入口PRODUCT_SELECTION_MANUAL_STEPS.md：流式导购业务拆为合并需求→真实候选→模型选编号→流式理由→Java校验/版本及短事务保存，本版3模型步骤（比旧2步多一次）；旧/api/ask保留。用户新selection_schema.ProductSelection(StrictInt列表≤5)、selection_service.select_products(复用AskRequest/规范合并条件/候选事实数据/JSON模式/合法id检查/保序去重)、main import+POST /api/recommendations/select(200 data编号/输入422/上游503)。助手仅tmp32候选预检，未代写正式Python。下一本组正式验收→已选商品解释流协议→Java消费与私有多轮保存/取消提交边界，真实流/语义仍待，前端暂停。

2026-10-19最新：正式91Java源码package、事件Codec/线程池42技术检查通过，详JAVA_STREAM_CODEC_EXECUTOR_ACCEPTANCE.md/verification/java-stream-codec-formal-2026-10-19.json；未代改正式源码/DB/依赖，正常8000/8888仍停，MySQL7888保持。当前手写入口JAVA_MODEL_STREAM_RELAY_MANUAL_STEPS.md（三组4文件）：AiClient原唯一streamChat替换兼容两参+三参onOpen(InputStream)并加import；新AiModelStreamService后台有界任务/每请求状态/累计校验/SseEmitter/完成超时错误取消显式关闭；新AdminAiStreamController POST /api/admin/ai/chat/stream复用ADMIN+CSRF；GlobalExceptionHandler原errorResponse helper加显式application/json与import以支持流前400/503。候选首轮29/30取消TCP失败保留，显式input.close修正后MVC真实30通过+旧两参27兼容，仅tmp不是正式功能。下一必须本组正式构建/JSON与CookieCSRF/事件/队列满/超时取消集中验收，再绑定商品导购私有多轮和保存；真模型/浏览器断连/Python到模型取消传播仍待，模型done不当落库成功，前端暂停。

2026-10-19最新：正式Java87源码package/流读取34技术检查通过（27正式class解析/loopback HTTP+7正式Java→实际Python HTTP→模拟provider，含提前delta/完整done/断流error），详JAVA_STREAM_READER_ACCEPTANCE.md与verification/java-stream-reader-formal-2026-10-19.json。无真实模型/DB/依赖或源码代改，临时全停，正常8000/8888仍停，MySQL7888保持；真实语义/模型流仍待配置环境。当前手写入口JAVA_STREAM_CODEC_EXECUTOR_MANUAL_STEPS.md，三步4新文件：dto.AiStreamDelta/content、AiStreamError/code/message；service.AiStreamJsonCodec注入原严格JsonMapper副本+trailing校验，读delta/done(复用AiChatData并与累计文本一致)/error固定安全文案，无请求共享状态；config.AiStreamConfig注册aiStreamExecutor核心2/上限4/队列8，由Spring初始化/关闭。助手仅tmp编译/42预检，不代写正式文件，前课AiClient不用再改。下一先本组正式验收再SseEmitter转发Service/Controller/安全，处理累计总长/结束状态/任务拒绝与取消；私有多轮/商品/原子保存后接，前端暂停。

2026-10-19最新：正式Python流式代码集中验收69/69通过（26实际模型流解析MockTransport、30实际main/事件模块、13真实临时Uvicorn HTTP含第一段提前到达/受控阻塞释放/断流error/关闭），详PYTHON_STREAM_ACCEPTANCE.md与verification/stream-*-formal-2026-10-19.json。当前终端无Key，真实上游0/真实流及多轮语义待；不是Java导购私有保存流完成。无业务源码代写/DB/依赖变更，临时服务全停，正常8000/8888仍停，MySQL7888保持。当前手写入口JAVA_STREAM_READER_MANUAL_STEPS.md：新dto.AiStreamEvent(event,data原JSON字符串)、新client.AiSseReader同步UTF8逐行/空行分帧/已知事件/结束前EOF错误/Consumer回调/关闭、新AiClient.streamChat(message,onEvent)及4import，复用原RestClient.exchange校验状态/MIME并在响应回调内消费。助手仅tmp编译3单元/27模拟预检，正式Java待用户手写集中验收。下一严格JSON事件字段与完整done/error校验→有界执行器/SseEmitter→正式HTTP/取消/私有多轮商品与保存接线，不直接信任原始data或Pythondone当落库成功，前端暂停。

2026-10-19当前手写入口STREAM_HTTP_MANUAL_STEPS.md：用户要求一次多教几步，合并3步为新stream_events.py（encode_event+model_stream_events包装delta/done/error、生成器finally关闭）、main.py响应import替换/新import/顶层POST /api/model/chat/stream复用ChatRequest、stream_http_probe.py发本机HTTP展示原始SSE行。前课model_stream_client.py和本组仍待正式集中验收，助手未逐步核查/代写正式源码/启动服务或模型DB调用，候选30项仅tmp预检通过。模型done只代表完整文字，不是Java保存；流已打开错误走error事件/HTTP可能200，前置输入422。下一组先正式Python全链技术验收与必要模型调用，再Java SSE私有协议/转发/校验和原子保存；断流取消与提交边界须验证，前端暂停。服务启停继续助手负责，用户手写后成组检查。正式多轮98技术通过与真实语义待验边界不变。

2026-10-19最新：用户将消息latestQuery改为LIMIT 1 FOR SHARE已核实，唯一业务源码变化；正式85源码package、修正后真实MySQL+实际Python/正式JAR的98针对性技术检查全通过，详MULTITURN_FORMAL_FIXED_ACCEPTANCE.md/verification/multiturn-formal-fixed-2026-10-19.json。空/非空各一200一409、旧RR当前读冲突、3阶段事务回滚和旧单轮均通过，未扩大权限或代改源码。新测试会话18～20/消息60～71共12条保留，原基线不变；临时全停，正常8000/8888仍停，MySQL7888/3306保持。真实DeepSeek多轮语义仍待（本轮上游付费0），不要据技术模拟称语义通过。当前手写入口STREAM_MODEL_CLIENT_MANUAL_STEP.md：用户新建ai-service/model_stream_client.py的ask_model_stream生成器，stream=True/httpx.stream/按行取delta.content/yield/双结束检查/2000字/关闭连接；原ask_model/结构化链不变，尚无HTTP/Java/网页流式。候选26预检仅tmp不算正式。下一Python SSE协议/路由→Java转发/完整校验保存→整组含断流并发与真实模型验收；前端暂停。

2026-10-19集中验收：85Java源码package成功；正式多轮首轮HTTP500，最新消息LIMIT 1 FOR UPDATE被MySQL8.0.34拒绝（chat_messages仅SELECT/INSERT）。当前入口MULTITURN_LOCK_FIX_MANUAL_STEP.md，用户只把latestQuery.last改为LIMIT 1 FOR SHARE，父会话query.last(FOR UPDATE)保持。助手未代改正式源码或扩大权限。隔离一行副本业务检查201通过/1测试标识断言过强，独立只读已澄清；推荐扩展39和正式旧AI78通过，真实模型语义仍待。详MULTITURN_ACCEPTANCE.md/verification/multiturn-summary-2026-10-19.json，不能将副本通过记为正式完成。复用声明会话12～17，保留22验收消息33～54，原业务基线不变，临时服务全停，正常8000/8888仍停，MySQL7888/3306保持。下一用户改一行→正式重建/保存与空非空409/RR/回滚复验→再SSE，前端暂停。
2026-10-19当前最后接线课JAVA_CONVERSATION_WIRING_MANUAL_STEP.md：完整两Service手写替换，单轮/多轮复用候选推荐，历史意图结果走澄清或真实商品，原问题/版本统一保存。下一轮优先整组构建/模拟+真实DB/并发409/旧接口兼容及必要真语义验收，不新增功能；当前仅教案未验收，前端暂停。

2026-10-19多轮推荐衔接当前课CONVERSATION_RECOMMENDATION_CONTEXT_MANUAL_STEP.md：可选结构化searchIntent传推荐端，原question与旧单轮请求保持；Python共用金额校验，AiClient两参重载。下一AssistantService/SendService直接消费合并条件和澄清分支，随后整组集中验收含409并发/旧协议；助手未代写运行，真实语义仍待，前端暂停。

2026-10-19当前课JAVA_CONVERSATION_CONFLICT_MANUAL_STEP.md：用户手写409异常/Advice、保存第5参数与会话锁内当前读最新id比较，并同步唯一正式调用/上下文读取，保持编译签名一致；无绕过旧重载。模型暂旧单轮，下一多轮编排/合并需求真实商品/澄清后集中验收含空/非空并发409。助手未代写运行，前端暂停。

2026-10-19当前课JAVA_CONVERSATION_CONTEXT_MANUAL_STEP.md：本人会话最近10条上下文+同批最新消息id（空0），读前归属/倒序限量/反转升序，用户手写DTO与ChatMessageService方法。下一保存锁内最新id当前读/409和签名迁移→多轮编排/合并需求推荐/澄清保存→整组验证，再SSE。助手仅文档无应用写入或运行，前端暂停。

2026-10-19多轮当前课JAVA_CONVERSATION_CLIENT_MANUAL_STEP.md：用户给AiClient新增上下文HTTP请求与双向协议校验，有效澄清不当异常，单轮方法保持。下一本人会话历史窗口/最新消息id、编排合并需求和澄清保存、锁内409冲突保护后集中验证，真实模型语义待原Key环境，前端暂停。此课未代写或运行，不按给代码记Java已接通。

2026-10-19多轮Python技术验收105通过，导入错误修好，真实模型未发请求（终端无Key配置），语义案例仍待。当前JAVA_CONVERSATION_DTO_MANUAL_STEP.md用户手写四record后端协议；下一AiClient/私有历史窗口/合并需求商品推荐/保存时409并发保护及整组验证，再SSE。无源码代写，前端暂停；技术验收与真实语义/Java接通分别记。

2026-10-18当前优先导入错误修复与PythonHTTP：CONVERSATION_IMPORT_FIX_AND_API_MANUAL_STEP.md定位schema被误覆盖/intent_service缺新函数，用户分别恢复/替换后main加新上下文路由。33临时副本预检通过不等于正式修复；助手未代改正式源码/调用模型。下一先本组正式Python验证，再Java多轮/409一致性保护接线，不跳过故障继续大功能，前端暂停。

2026-10-18多轮当前课CONVERSATION_CONTEXT_SERVICE_MANUAL_STEP.md：用户提取单/多轮共享金额规范化，新增当前问题+历史数据提示的提取Service/澄清回退；明确规则与真实验证区分。下一Python HTTP入口后集中本组/旧单轮验证，再Java私有窗口/协议/推荐/409冲突保护接线，整组多轮通过后SSE。助手本轮仅教案，未调用模型或代写，前端暂停。

2026-10-18阶段6多轮第一课CONVERSATION_CONTEXT_SCHEMA_MANUAL_STEP.md：用户新建有界历史请求/可空澄清结果Python schema，沿当前导购事实链；明确预算更新可承接、模糊便宜不猜金额。下一上下文Service/路由/Python验证→Java私有历史读取/客户端/合并条件推荐及保存时最新消息id冲突保护→整组验收，再SSE/Agent购物车。单轮287保持，多轮未实现，助手本轮仅文档，前端学习暂停。

2026-10-18阶段6会话内单轮问答保存已验收：79源码、209本组+78原AI模拟=287通过，详CHAT_MESSAGE_SEND_ACCEPTANCE.md。真实DB/Cookie/正式JAR，模拟模型，无本轮付费或源码代写，保留登记4会话20样例/原业务不变。下一多轮上下文需求和协议，再SSE/Agent购物车/页面接线演示；持久化不当多轮，Nginx/Redis后续目标保持，前端基础学习暂停。

2026-10-18阶段6当前课CHAT_MESSAGE_SEND_CONTROLLER_MANUAL_STEP.md：已有Controller替换唯一构造器并接POST消息，Security精确USER POST增量/CSRF保持。下一优先整组正式构建/持久化回读/JSON兼容/身份隔离/故障回滚验收，不继续新功能；目前仅手写教案，60%～65%功能首版估计口径保持（排除部署扩展）。再多轮/SSE、Agent购物车、页面接线与整体验收。

2026-10-18阶段6当前手写课CHAT_MESSAGE_SEND_SERVICE_MANUAL_STEP.md：单个编排Service串联本人归属/校验→已有单轮导购→商品展示DTO→独立写Bean短事务保存，不包模型事务，不等于已接多轮。下一Controller/精确POST安全→整组真实保存/故障/隔离/JSON历史兼容验收，再多轮/SSE；助手本轮只文档，未代写运行，前端暂停。

2026-10-18阶段6当前手写课CHAT_MESSAGE_WRITE_SERVICE_MANUAL_STEP.md：短事务保存一问一答、推荐JSON和会话时间；FOR UPDATE锁本人会话串行保存，私有helper在同一事务。下一事务外导购编排→发送HTTP/安全→整组持久化/故障/旧读取兼容验收，再多轮/SSE。助手未代写或运行，前课/当前课均不因继续记为验收完成，前端暂停。

2026-10-18阶段6当前手写课CHAT_MESSAGE_RECOMMENDATION_JSON_MANUAL_STEP.md：显式JSON转换器/5字段MessageResponse/历史Service转换接线，旧SQL NULL继续文字历史，推荐快照恢复对象不重新跑模型。用户手写，助手仅文档与依赖签名核对，未正式构建/迁移或运行。本组下一短事务保存→事务外导购编排→发送入口/安全→集中兼容/事务/持久化/故障验收，再多轮/SSE，前端暂停。

2026-10-18阶段6当前课CHAT_MESSAGE_RECOMMENDATION_STORAGE_MANUAL_STEP.md：用户手写011给chat_messages新增可空JSON结果快照、ChatMessage新增String映射/访问器，旧消息null兼容。只教案未执行迁移或应用修改，不逐步验收。下一结构转换组件/历史响应扩展→短事务保存与事务外导购编排→入口安全与集中验收，再多轮/SSE，保持购物导购和卡片而非纯聊天路线。

2026-10-18阶段6当前手写课CHAT_MESSAGE_SEND_DTO_MANUAL_STEP.md：先两个发送请求/响应DTO，明确复用AssistantService.ask及结构化推荐。下一JSON结果存储/实体和历史响应→短事务成对保存与事务外导购编排→发送Controller/ROLE_USER与CSRF→集中验收，再多轮/SSE。目标模型失败不写/保存失败整对回滚，暂不承诺幂等或并发多轮顺序；本课未实现HTTP/DB/模型行为，前端继续暂停。

2026-10-18阶段6列表/消息读取正式验收：74源码构建、105本组+78原AI模拟回归=183通过，见CHAT_HISTORY_READ_ACCEPTANCE.md；首次21失败为辅助程序编码，修正仅辅助输出并只读复用样例验证，无业务改写。用户反馈绕，先以CHAT_HISTORY_REQUEST_FLOW.md串起我的列表/点开消息和各层职责。下一消息发送/保存契约及失败策略→现有问答落库→多轮/SSE，前端暂停，当前读取已完成但真实消息发送保存未完成。

2026-10-18阶段6当前课CHAT_MESSAGE_HISTORY_CONTROLLER_MANUAL_STEP.md：用户给既有Controller替换双Service构造器并新增消息GET，Security精确USER消息模式；不重复原三个方法。下一轮先列表+消息读取整组集中验收，登记必要测试消息、无收费调用，旧275不替代本轮结果；通过后消息写入/多轮SSE，前端暂停。

2026-10-18阶段6当前课CHAT_MESSAGE_HISTORY_SERVICE_MANUAL_STEP.md：用户手写消息历史Service，先复用getOwned归属检查，cursor筛选+现有Page限size+1无count，截额外/反转/响应游标。当前未接HTTP或运行，不当消息保存/AI上下文已完成；下一Controller构造器注入替换与精确消息GET规则后和列表集中验收，前端暂停。

2026-10-18阶段6当前课CHAT_MESSAGE_HISTORY_DTO_MANUAL_STEP.md：用户手写消息响应和历史分页两个DTO，目标按beforeId取更早消息（size默认20/1..50，返回items/nextBeforeId/hasMore），父会话先验归属。列表继续前课page/size；Controller列表已存在不重复加。下一消息Service/入口和精确USER规则后与列表一起集中验收，当前无运行/消息保存/多轮，前端暂停。

2026-10-18阶段6当前课CHAT_CONVERSATION_LIST_MANUAL_STEP.md：用户按完整4处代码手写本人分页列表DTO/Service/Controller/Security，沿商品分页布局；page默认1/size10且1..50，总数和条目都限当前user_id，更新时间/id倒序。当前仅教案，无运行验收，下一消息读取分页接线后与列表集中隔离/边界验证，再消息保存和多轮SSE，前端暂停。

2026-10-18阶段6私有会话创建/本人详情正式验收：70源码构建、101会话检查（含8模拟身份边界）+69登录+27注册输入+78AI模拟回归共275通过，见CHAT_CONVERSATION_ACCEPTANCE.md。真实两用户Cookie隔离、标题/时间/CSRF/角色/事务回滚与独立断连503验证，保留3练习会话无新用户/商品/消息。下一GET会话分页列表→消息保存/读取→多轮/SSE；当前只完成会话容器，网页接线待做、前端学习暂停，不按测试数量增加百分比。

2026-10-18阶段6当前课CHAT_CONVERSATION_CONTROLLER_MANUAL_STEP.md：用户手写创建/本人详情Controller及精确ROLE_USER规则，不开放新列表/消息路径、保留CSRF。下一轮先集中正式验收这组SQL/授权/当前身份/DTO/创建读取/隔离及旧接口，登记练习会话；本课只教案未运行，不把接线当已验收。通过后继续列表/消息、多轮/SSE，前端暂停。

2026-10-18阶段6当前课CHAT_CONVERSATION_READ_SERVICE_MANUAL_STEP.md：用户手写本人读取getOwned和专门404异常/映射，URL会话id+服务端当前用户id同时限制，不存在与非本人同404，复用响应DTO。下一Controller和USER安全规则后先集中创建读取最小闭环验收，再分页列表/消息与多轮SSE；本轮仅文档未接线运行，前端暂停。

2026-10-18阶段6当前课CHAT_CONVERSATION_CREATE_SERVICE_MANUAL_STEP.md：用户手写会话创建Service，短事务内确定归属/校验标题/插入并读回默认时间/组DTO，不代写或当作HTTP已开放。下一本人会话读取404、Controller/Security，再分页列表/消息和集中权限持久化归属验收，前端暂停。

2026-10-18阶段6当前课CHAT_CONVERSATION_DTO_MANUAL_STEP.md：先区分用户/会话/消息三类id，创建会话契约和两个DTO手写；POST JSON title→ROLE_USER/CSRF→服务端确定userId→INSERT生成会话id→读回默认时间→201 data响应/Location。下一Service/Controller和本人详情→分页列表/消息读契约业务→集中表权限与隔离验收；当前未接线运行，前端暂停。

2026-10-18阶段6当前课CHAT_CURRENT_USER_MANUAL_STEP.md：用户手写可信当前身份→普通用户id服务，专门401全局映射，防匿名/账号缺失禁用与通用500混淆，不复制参考的name转id。下一完整私有会话HTTP契约/DTO→业务/入口和ROLE_USER→集中SQL/授权/Java/归属验收；当前只有教案无接线运行，前端暂停。

2026-10-18阶段6当前课CHAT_MESSAGE_MAPPERS_MANUAL_STEP.md：用户手写消息实体+会话和消息两个Mapper，沿已有AppUser布局，不一次生成完整历史业务。下一可信当前用户id服务→会话创建/分页列表/消息读取契约及业务→集中表/授权/Java和跨用户隔离，继续多轮/SSE。助手未代写或运行验收，前端暂停，进度不因新增教案变化。

2026-10-18阶段6当前课CHAT_CONVERSATION_ENTITY_MANUAL_STEP.md：针对用户对建表/授权的困惑，先串联存储结构/Java账号权限/业务用户归属，再单文件ChatConversation实体手写。未执行SQL或代写应用，表/权限/本课源码待分组验收。下一消息实体/Mapper、可信用户id、私有历史接口，再多轮/SSE；前端暂停，功能粗估口径保持。

2026-10-18阶段6当前课CHAT_HISTORY_GRANTS_MANUAL_STEP.md：用户手写010表级权限，会话SELECT/INSERT/UPDATE，消息SELECT/INSERT；保留现有账号和其他表权限。009文件已存在，表执行与授权待集中验收，助手未执行数据库写或逐步测试。下一聊天实体Mapper/可信用户id/接口契约与业务，再集中归属/持久化验证，继续多轮SSE。前端学习暂停，不因SQL教案增加功能完成比例。

2026-10-18阶段6当前课CHAT_HISTORY_TABLES_MANUAL_STEP.md：登录后端已验收后，进入用户会话/消息两表，给用户手写009完整DDL和DataGrip执行说明，助手仅只读元数据核对/文档。普通USER账号归属，列/取会话按当前app_users.id校验；会话业务历史独立于登录Session。下一最小权限→实体/Mapper与可信用户id→私有会话/历史接口→集中隔离验收，再多轮/SSE；新表和接口未当完成，功能60%～65%口径保持，前端暂停。

2026-10-18阶段6登录/me/logout最小后端正式验收：60源码package成功、69会话HTTP+6Provider模拟Mapper+27注册输入+78AI模拟回归共180通过，见AUTH_SESSION_ACCEPTANCE.md；前端账号页面/私有历史/多轮/SSE尚未完成。下一会话/消息设计及用户归属，再历史多轮流式/Agent购物车。用户询问排除Nginx/多实例/RedisCluster等扩展的功能首版口径，主观估60%～65%；完整订单支付物流未确定不计入分母，部署扩展未被取消。此前45%～50%是包含扩展的完整路线估计，不按测试数增加百分比。

2026-10-18阶段6当前课AUTH_LOGOUT_MANUAL_STEP.md：用户手写Security POST退出配置，Session/认证清理、移除JSESSIONID、204，保留CSRF并说明登录后/退出后刷新令牌；不增Controller。下一对注册后整组登录身份/me/logout做集中正式构建与HTTP会话安全/旧接口验收，通过后私有会话/历史；本课只教案与API核对，不算功能完成，前端暂停。

2026-10-18阶段6当前课AUTH_CURRENT_USER_MANUAL_STEP.md：用户手写当前登录用户名GET /api/auth/me与精确authenticated规则；从可信Authentication取username，非请求参数。下一POST退出后集中验收登录三接口/会话与旧功能；未构建运行，不将上课配置出现或新教案算登录完成，前端暂停。

2026-10-18阶段6当前课AUTH_FORM_LOGIN_MANUAL_STEP.md：用户手写SecurityConfig表单登录/Session与成功失败处理器绑定，登录前输入边界/CSRF保留和成功后Session编号更换。该配置与前课正式文件待集中检查，不记已登录；下一me/退出接线后正式Cookie认证/授权/CSRF和既有接口验收，前端暂停。参考JWT不照搬，当前先服务端会话，共享存储后续多实例阶段处理。

2026-10-18阶段6最新课AUTH_LOGIN_HANDLERS_MANUAL_STEP.md：按同域网页选Security Session/Cookie登录，框架formLogin表单接收账号密码，未来共享存储随多实例课接入。协议先定login200/me当前身份/logout204，本课只成功/失败两个Component教案，尚无路由/会话运行。下一配置输入边界/CSRF/会话和结果绑定，再me/退出集中验收。注册118历史保持，前端暂停，不把回调对象或方案选择当登录完成。

2026-10-18当前阶段6课AUTH_LOGIN_WIRING_MANUAL_STEP.md：用户手写组合UserDetailsService Bean，将普通用户DB查询和原管理员读配置接到同一认证入口，401提示通用化。新Bean/user读取正式代码待集中检查，未实现登录会话；Basic正确USER访问ADMIN应403，不沿用旧401断言。下一确定登录/当前用户/退出HTTP及会话方案、接线后集中验收，再私有会话/多轮/SSE。注册验收历史保持，整体45%~50%仍为主观粗估，前端暂停。

2026-10-18最新注册收口与进度：正式57源码构建、本轮27+3+78+10=118针对性检查全通过，注册最小闭环已验收（历史失败保留）。整体完整路线按剩余工作量主观粗估45%~50%，不是阶段号/代码量/工时统计；首版导购/Web和RAG后端已形成，登录私有会话/多轮/SSE、购物车Agent、Nginx多实例/RedisCluster与部署展示仍后续。当前课AUTH_LOGIN_USER_LOOKUP_MANUAL_STEP.md用户手写AppUserDetailsService类，下一统一接Security现有Bean并定登录协议；不把新教案算已登录，前端暂停。

2026-10-18当前阶段6入口AUTH_REGISTER_FIX_MANUAL_STEP.md：正式56源码构建，注册58项55通过，2处配置遗漏对应3失败，见AUTH_REGISTER_ACCEPTANCE.md。正常/权限/并发/哈希链已通过，网站输入数字→String与事务创建连接异常映射待用户手写修复；隔离方案27+3+78检查通过不当正式完成。已登记6测试用户，无凭据/模型调用或既有数据改动；下一正式修复验收后登录，当前不跳过失败推进身份。

2026-10-18阶段6当前课AUTH_REGISTER_DB_GRANT_MANUAL_STEP.md：008授权教案只给app_users SELECT/INSERT（当前注册实际需要，UPDATE以后需修改再加），用户用DataGrip root执行/保存脚本。助手未写正式008或执行授权；此前表/Java操作仍待下一轮集中核对、编译、注册/安全/并发/哈希及既有接口回归。注册验收后进入登录身份，前端暂停。

2026-10-18阶段6当前课AUTH_REGISTER_CONTROLLER_MANUAL_STEP.md：用户手写注册HTTP入口、Security精确POST放行保留CSRF、DB异常通用提示。新入口201/400/409/403/503仍待集中运行确认；下一008仅app_users最小权限，再一次核对建表与这几课正式源码/构建和注册安全/存储回归，随后登录身份。助手本轮仅教案，无正式业务/DB/模型变更，前端暂停。

2026-10-18阶段6当前课AUTH_REGISTER_SERVICE_MANUAL_STEP.md：用户手写重名业务异常/409处理与AuthService（保留名/密码字节检查、查重、编码/插入、并发唯一冲突兜底、事务和响应DTO）。正式表/上课文件/本课仍待集中验收，不因给出代码记注册成功。下一注册Controller/Security/DB提示及最小权限后集中核验，再身份/私有会话/多轮/SSE；前端暂停。

2026-10-18当前阶段6课AUTH_REGISTER_DTO_MANUAL_STEP.md：先明确注册POST /api/auth/register草案（201/400/409/403/503、注册不自动登录），用户手写RegisterRequest与UserResponse。正式表/映射/新DTO待集中验收；本轮只文档无源码/DB/运行变更。下一Service冲突处理/编码插入→Controller/安全与最小权限→集中注册验收→身份/私有会话/多轮/SSE；不新增前端学习前置。

2026-10-18当前课AUTH_USER_MAPPING_MANUAL_STEP.md：普通用户AppUser实体与BaseMapper接口，照已有商品MyBatis-Plus布局用户手写。表执行与新Java文件保留待集中验收；本轮仅教案文档，没有源码/DB/运行改动。下一注册HTTP协议/请求及响应DTO→Service编码密码/查重/插入→入口/安全与最小数据库权限，再集中核验；身份与私有会话/多轮/SSE仍按阶段6顺序。

2026-10-18最新恢复：正式AiClientConfig用户补齐后49源码package与新正式JAR78项集中模拟/安全/旧AI回归全通过；阶段5最小后端知识链验收收口，证据JAVA_KNOWLEDGE_ACCEPTANCE.md及verification/java-knowledge-formal-fixed-2026-10-18.json。本轮未重复收费调用，历史真实3样例仍按原范围。进入阶段6设计课AUTH_USER_TABLE_MANUAL_STEP.md：普通账号表（仅教案，未执行）→映射/注册→身份→私有会话→多轮→SSE；先不新增整套业务，前端暂停不作为前置。旧失败和候选记录仅为历史。

2026-10-18最新里程碑：Java知识正常真实全链3样例通过，正式49源码构建成功；60模拟/安全检查57通过，3项Textual类型转换问题待修复。隔离候选78项通过但未落正式源码；当前唯一手写入口JAVA_KNOWLEDGE_TEXTUAL_FIX_MANUAL_STEP.md，用户补配置后助手重建/重启/回归，不重复收费样例除非新问题。当前先修复验收缺陷再推进身份/多轮需求，前端保持暂停，不把正常链成功当阶段5全部完成。详情JAVA_KNOWLEDGE_ACCEPTANCE.md。

2026-10-18阶段5最新手写课：JAVA_KNOWLEDGE_CONTROLLER_MANUAL_STEP.md，新建知识Controller与SecurityConfig精确POST放行增量，保持CSRF和管理员边界。下一Java本轮集中核对/构建/HTTP模拟及少量真实依据样例；当前只给教案，未把用户操作或Java知识链记验收通过，前端继续暂停。

2026-10-18阶段5当前入口：JAVA_KNOWLEDGE_CLIENT_MANUAL_STEP.md，用户手写AiClient知识调用与完整辅助校验方法；新Java正式代码/编译尚待集中验收，不把教案算接通。下一Controller/安全接线后检查完整Java→Python链和异常协议，Python历史70项已有证据。继续暂停前端编码学习。

2026-10-17阶段5当前里程碑：Python知识检索/生成/引用/HTTP正式链70项检查通过（59模拟+11运行，其中3真实模型样例已逐条依据评审），见RAG_PYTHON_ACCEPTANCE.md。当前JAVA_KNOWLEDGE_DTO_MANUAL_STEP.md用户手写三个record，随后Java客户端协议复核、对外入口及安全接线、集中联调；阶段5仍进行中，未把Python完成当全项目/RAG全部完成。前端学习保持暂缓。

2026-10-17阶段5最新手写课：RAG_API_MANUAL_STEP.md，在Python main.py加一个import与知识HTTP路由，保持既有输入/成功data/503风格；正式路由尚待用户手写和集中验收。下一检查检索/schema/service/main、模拟边界/真实少量知识依据案例，必要服务重启助手直接处理；Python过关再Java，不以教案视为完成。

2026-10-17阶段5最新入口：RAG_SERVICE_MANUAL_STEP.md，用户手写知识Service，用本次检索候选构造提示JSON、读模型草稿、核对引用并组装来源；当前仅教案。下一Python知识HTTP路由与集中验收，然后Java衔接。JSON与候选编号校验不当作全文事实验证，未知问题拒答仍需真实案例检查；不自动扩大输出预算或代写正式业务。

2026-10-17阶段5最新入口：RAG_SCHEMA_MANUAL_STEP.md与API_CONTRACT顶部知识问答草案，用户手写模型草稿/引用/业务结果三类；后续Service检查候选引用并组装真实来源，之后Python HTTP/Java接线。当前仅定义结构，尚未生成带依据回答，模型可答标志/合法引用也不当全文事实保证；不改变既有推荐和安全配置。

2026-10-17阶段5最新手写入口：RAG_SEARCH_MANUAL_STEP.md，FAISS内存IndexFlatIP+归一化向量检索，不改现有推荐。用户前三模块本轮集中运行通过，FAISS依赖已装并固定；新检索候选隔离验证通过，正式新文件用户手写待验收。下一带引用/资料不足回答协议与生成，再独立知识问答HTTP和Java衔接；尚未完成知识问答，不用最近向量分数冒充答案依据。

2026-10-17阶段5最新：RAG_EMBEDDING_MANUAL_STEP.md。选择本地BGE小中文模型+Sentence Transformers CPU，沿参考支持的本地embedding方向。助手已在项目虚拟环境安装/固定依赖、下载校验D盘权重并通过隔离向量探针，未代写正式应用模块；当前用户手写embedding_client.py，下一知识片段向量检索与索引。DeepSeek继续回答，Dify非必需；环境探针不记为RAG或用户源码验收。

2026-10-17阶段5最新手写入口：RAG_SPLITTER_MANUAL_STEP.md，knowledge_splitter.py按当前短文二级标题切分，携带来源/章节/文档演示声明；用户编码与运行待集中检查。下一选择向量化和索引依赖，切分教案不等于RAG运行完成；既定接口设计在对外路由编码前补齐，暂不改已有推荐入口。

2026-10-17阶段5当前小步：RAG_DOCUMENT_MANUAL_STEP.md，先用户手写一份虚构耳机演示知识资料与pathlib读取模块，用3个可人工核对的问题限定验收；下一切分与来源信息，再确定向量化/检索依赖、引用回答与接口。当前仅给教案，不是RAG已实现；不改商品推荐/价格事实，不安装Dify或提前批量生成模块。

2026-10-17最新教学安排覆盖下文前端讲解入口：用户要求暂时跳过前端，保留当前页面；不以学习HTML/CSS/JavaScript作为后续后端开发前置条件。当前转到阶段5最小知识问答场景→资料准备→接口设计，再按小步手写Python/Java；用Swagger或HTTP工具联调，后续必要页面接线由助手承担。WEB_REQUEST_FLOW_STEP理解暂缓，主路线与业务范围保持。

2026-10-17定位澄清：原参考是带商品、账号、购物车、订单等电商模块的AI Agent项目；我们自己的方向是带电商业务基础的AI购物助手。当前商品CRUD与首版导购只是已完成的一部分，后续知识问答、身份/会话和Agent购物车继续按下表推进。完整订单与支付仍未确定纳入，不能称已完成电商交易平台，也不因这次提问自动扩展范围。当前课WEB_REQUEST_FLOW_STEP.md，讲解play.html→app.js→AssistantController→AssistantService→页面渲染，随后阶段5场景/资料/接口设计。

2026-10-17最新选择覆盖下文待选记录：用户确定跳糖选物作为默认首页，其他九个版本（含最初绿色页）全部保留。入口与切换说明见FRONTEND_THEMES.md，后续前端以play.html继续完善；本次不增加后端功能、不改变阶段4和后续路线。

2026-10-17最新第二轮前端：在保留对话工作台/黑曜科技馆的基础上，新做六个风格候选，含四个Ark风格探索与额外两种MiMo参考。九版对比入口已更新，正式同域资源和真实推荐验收见FRONTEND_DESIGN_ROUND2.md。仍待用户选主风格，阶段4及后续路线不变。

2026-10-17最新视觉迭代：按用户要求安装并应用ui-ux-pro-max与taste-skill，新建三套同域可操作网页与designs.html对比入口。原版和业务源码保留，实际筛选/详情/单次真实推荐及四种屏宽检查见FRONTEND_DESIGN_VARIANTS.md；等待用户选主风格后继续阶段4教学与完善，不把视觉候选计成后续功能完成。

更新时间：2026-10-17（Asia/Hong_Kong）。路线版本：0.3。本文件是全流程教学路线的唯一主文档。商品CRUD、跨服务与推荐接口链已形成；首版同域HTML/CSS/JS网页已实现并完成真实浏览器导购/商品详情演示、27项HTTP断言、桌面与手机宽度/隔离故障测试，见WEB_FIRST_VERSION_ACCEPTANCE.md。当前处于阶段4，等待用户体验反馈与教学回顾，正式React/Vite未实现；后续RAG、用户会话/流式、Agent购物车、Nginx多实例/RedisCluster与部署展示保持既定路线。推荐措辞继续用真实样例评审，不作普适准确率保证。

长期目标：从 `D:\ShopAgent-X` 开始，参考 ShopAgent-X，从零实现用户自己的第一个简历项目，能够理解、修改、演示并解释自己的实现。前端使用 vibe coding；Java / Python 按实际开发手把手教学。每次只推进一个可验证的小目标。

2026-10-17进度沟通：按当前已验收成果与剩余工作量主观粗估，完整既定路线约35%、首个可演示导购/Web约65%；并非工时/代码量统计，阶段及验收证据优先。当前推荐schema/service用户已写但未运行验收，正教学Python /api/ask，随后Python集中验收→Java推荐衔接/编号复核→Web与整体回归；RAG、身份/多轮/流式、Agent购物车、Nginx多实例/RedisCluster和部署展示仍后续，不因给出教案就增加已验收功能。

## 1. 先说清项目要解决什么问题

2026-10-17最新：用户严格JSON配置已进入正式45源码构建，24项定向回归全部通过，编号转型失败关闭；Java推荐接口技术链闭环，真实200为修正前样例，修后读取与旧AI兼容已模拟回归，无额外模型请求。见JAVA_RECOMMENDATION_ACCEPTANCE.md。当前手动收紧推荐比较措辞，随后首版Web/详情联动与整体演示验收，阶段4仍进行中，不把接口完成当页面完成。

2026-10-17阶段4最新验收：Java完整推荐45源码构建与真实正常链通过，但集中检查59通过/1失败（小数编号被截断误认真实商品），当前待手动修正AI客户端JSON转换及正式针对性回归，见JAVA_RECOMMENDATION_ACCEPTANCE.md。隔离候选配置7项通过不当正式修复；编号失败和Python文字依据待办关闭后再进入Web，阶段4仍进行中。

2026-10-17分端进度沟通：仅首版单轮导购后端按剩余工作量主观粗估Python约90%、Java约80%；Python接口/提取/推荐已验收且尚需文本措辞收紧与全链回归，Java剩推荐客户端/编排/Controller与全链验收。不是完整项目百分比、工时统计或功能自动验收，本轮仅给AiClient推荐手写课，不依据给出代码增加已完成项。

2026-10-17阶段4补充：Python推荐/api/ask已由用户手写并通过79模拟+1运行入口+2真实样例共82项检查；Java二次推荐衔接和Web尚未实现。推荐结构/编号受约束，真实措辞中无依据性能比较仍需收紧/评审，见RECOMMENDATION_ACCEPTANCE.md。继续按交付里程碑报告，不因新增测试数量机械增加完成百分比。

ShopAgent 是一个 AI 导购应用：用户用自然语言描述商品需求，系统查自己的商品数据，给出有依据的推荐与商品卡片；后续支持多轮追问和用户确认后的购物车操作。

例如：用户说“想买一款五百元以内的跑鞋”，系统要推荐数据库中真实存在、符合预算的商品。价格和商品 ID 来自业务数据，模型负责理解需求和组织说明。没有符合条件的商品时，明确告知无匹配。

当前建议先做 Web + Java + Python，前端按用户偏好由 AI 辅助实现；Android、多模态、完整订单体系保留为后续选项，没有最终取消。首个可演示版本目标：输入需求 → 查询真实商品 → AI 说明推荐理由 → 展示卡片 → 打开详情。

### 服务分工

| 部分 | 我们主要做什么 | 学习重点 |
| --- | --- | --- |
| Web 页面 | 展示商品、输入需求、展示回答与卡片 | 接口调用、数据状态、浏览器排错 |
| Java / Spring Boot | 商品查询、业务校验；后续账号、会话、购物车 | Controller → Service → Mapper → 数据库 |
| Python / FastAPI | 模型调用、需求理解；后续检索与工具编排 | Java → Python → 模型/检索 → 结构化结果 |
| MySQL | 商品与业务事实 | 字段含义、查询与约束 |
| Redis / 向量索引 | 对应阶段再加入会话缓存与知识检索 | 为什么需要、失效/检索如何验证 |

参考项目的 Python 会直接查询 MySQL。我们自己的商品读取边界，在跨服务阶段比较“Java 提供候选商品”与“Python 查询商品”后确定；当前不会把建议说成已经实现的架构。

## 2. 四份参考指南怎样用

已全文阅读四份文档；本轮重新核对以下章节与对应源码。参考固定提交：`e45e59402206beb262d0fc8d007ee9b7a232b644`。全文阅读不等于运行验证。

| 参考文件 | 内容与用途 | 对应章节 |
| --- | --- | --- |
| `docs/设计文档.md` | 系统分工、请求链路、数据库、模块设计、接口清单 | 第 1、5、7、9 章 |
| `docs/部署与体验指南.md` | 环境、服务启动、商品推荐/多轮/购物车体验与检查清单 | 第 2、3、4 章与附录 |
| `ShopAgent-X学习指南.md` | 前置知识、按请求读代码、Agent 核心、验证方法、面试素材 | 第 3、5、6、7 章 |
| `新手友好食用指南.md` | 背景概念、目录、入口阅读、实践建议 | 第 2、4、5、7、8 章 |

用户提到的“新手养成实践指南”在该参考版本中的实际文件名是 `新手友好食用指南.md`。

两份学习指南的路线主要是先运行成品，再读代码和修改功能；我们的目标是从零理解并实现。借用它们的概念、模块和验收场景，按自己的功能开发顺序教学，不以文档中的天数/周数承诺学习周期。演示视频可先帮助认识目标界面，完整运行参考项目仍是待办，不阻止自己的第一课。

## 3. 开始编码前的轻量设计

先完成三项，并在对应功能开发前补齐细节：

1. 需求：用户输入什么、页面显示什么、怎样算做对。第一阶段先是商品列表/详情，首个导购版本是需求 → 真实商品 → 推荐解释。
2. 数据设计：商品阶段先画小范围字段与关系，准备 5～10 个能人工核对的样例。字段设计完成后再建表；不一次导入参考项目全部业务表。
3. 接口约定：先写当前阶段的地址、方法、参数、返回示例、错误与验收。第一版见 `API_CONTRACT.md`；后续逐模块补充。

接口文档解决“谁向谁发送什么，以及返回什么”。前端 → Java 与 Java → Python 是两份不同的约定。Swagger 等生成工具帮助展示代码已有的接口，不能代替需求与参数含义的讨论。

## 4. 实际开发顺序与验收

以下为当前教学建议。每一行都按“场景 → 小范围设计 → 编码 → 调接口 → 页面/服务联调 → 检查失败情况 → 保存进度”推进。

| 阶段 | 本阶段交付 | 验收标准 |
| --- | --- | --- |
| 0：认识项目与约定 | 说明用途、画服务分工、看懂一份接口示例 | 用户能解释页面、Java、Python 各做什么 |
| 1：Java 服务起步 | 为本项目选择 JDK 17，建立服务，提供 `GET /api/health` | 本机启动成功，浏览器收到约定 JSON；解释启动类与 Controller |
| 2a：商品查询基础 | 先定少量商品字段，再建独立库/表与种子数据；列表、详情、简单关键词搜索 | 与数据库比对；分页、无结果、错误参数、不存在/下架商品行为正确 |
| 2b：商品管理CRUD | 管理员访问控制；管理查询含下架；新增/修改/上下架/逻辑删除 | 认证授权与字段校验通过；CRUD端到端及公开查询回归；保留原种子，写权限只限本项目products |
| 3：Python 与跨服务调用 | 独立虚拟环境；先固定响应检查 Java → Python，再接真实模型 | 证明实际跨服务与模型调用；Python 未启动、超时、模型失败有明确结果；固定响应不计作 AI 功能 |
| 4：首个导购版本与 Web | 需求字段、真实候选商品、推荐说明、卡片与详情；前端由 AI 辅助实现 | 预算/品类约束可核对；卡片来自实际商品；无匹配时不虚构；页面能走完整流程 |
| 5：知识检索 / RAG | 区分商品事实和知识文档，完成切分、向量检索、引用 | 已知问题能引用对应材料；无材料时说明不足；价格仍以业务数据为准 |
| 6：身份、会话与流式 | 最小网页链已验收 | 原21业务/89HTTP及工作台检查保留，2026-10-20真实模型两轮消息保存/流/刷新恢复已演示，见KNOWLEDGE_WEB_AND_REAL_DEMO_ACCEPTANCE.md；样例不代表普适语义效果或用户已掌握全部原理 |
| 7：Agent 工具与购物车 | 最小购物车工具链已验收 | 原226后端/36工作台/48商城检查保留；2026-10-20真实模型意图→PENDING→人工确认→刷新历史→独立cart整链通过。本轮27实际链/25受控UI/4只读复核见KNOWLEDGE_WEB_AND_REAL_DEMO_ACCEPTANCE.md。LangGraph图编排、订单支付未实现；下一共享会话与部署 |
| 8：部署复现、展示与简历验收 | 可复现部署；Java多实例/Nginx负载均衡；RedisCluster演练；README、演示、贡献台账 | 多实例路由与停一实例恢复可核对；Redis分片和主从切换有实测；业务/失败可复现；指标来自自己的测量 |

账号阶段可以提前；在真实用户使用、保存私有数据或修改购物车前必须完成身份与权限校验。完整订单、支付、Android、图片与语音在主流程稳定后再讨论。

## 5. 每课怎样教

每课先解释一个实际问题，再讲这次必需的概念，给出少量代码并说明完整文件位置、谁调用谁、配置如何生效。用户操作后检查真实结果；出错就定位当前链路，不跳到后续模块。

每个功能结束：同步接口约定和运行证据，更新 `PROJECT_STATE.md`、`LEARNING_LOG.md`；需要时按用户授权范围提交 Git。没有实际执行的构建、模型请求、测试和推送不写成成功。

不要求前端先系统学完 React；仍会解释请求参数、响应字段、加载/失败状态。Java/Python 的关键代码以用户能理解和修改为目标。

## 6. 文档与代码有差异，开课前先核对

静态源码已确认：

- 设计文档第 9.3 节写 Python `/api/chat`、`/api/chat/stream`；实际 `api/routes.py` 定义 `/ask`、`/ask/stream`，`main.py` 加 `/api` 前缀，得到 `/api/ask`、`/api/ask/stream`。
- 设计文档把分类列表写作 `/api/product/categories`；实际 `CategoryController` 定义 `GET /api/category/list`。
- 设计文档列历史消息为 `/api/chat/conversations/{id}/messages`；实际 `ChatController` 在 `/api/chat` 下定义 `GET /messages`，需要结合方法参数核对调用。
- Java `pom.xml` 引入 `springdoc-openapi-starter-webmvc-ui` 2.3.0；部署指南介绍 Python `/docs`。参考服务尚未运行，不能宣称这些页面已在本机访问成功。

因此每个模块都按源码确定参考事实，再设计我们自己的协议；地址变化会明确说明，不让前端拿原地址调用新实现。

## 7. 当前下一步

正式启动检查已由用户创建，助手运行实际 JAR 验证商品读取/健康成功，只关闭自己的随机端口进程。当前见 PRODUCT_SERVICE_STEP.md：指导用户删除临时类、创建按编号且上架状态查询的 ProductService；候选已验证，正式 Service 待创建。随后详情 Controller/DTO/错误处理，再分页列表；均在阶段 2 内推进，HTTP 验收与理解反馈待完成。

## 8. 路线约束与变更记录

### 每次恢复先读什么

1. `../AGENTS.md`：教学约定与恢复规则。
2. `../PROJECT_STATE.md`：现在做到哪里、有哪些问题、下一课入口。
3. 本文件：长期目标与阶段顺序；它是路线的唯一主版本。
4. 本课涉及的 `API_CONTRACT.md`、环境/阅读记录和真实源码。

`PROJECT_STATE.md` 只保存当前进度和指向本路线，不另维护一套阶段表。`LEARNING_LOG.md` 保存每课结果与重要决定，不能把探索中的想法自动变成既定方案。导出到聊天的文件为查看副本，`D:\ShopAgent-X` 中的原文件为后续更新依据。

### 哪些事情不能在长聊中悄悄改变

- 长期目标仍是从零实现并理解自己的 ShopAgent 简历项目。临时解释某个概念、排查环境或回答一个问题，不等于替换目标。
- 按实际开发推进：需求 → 数据与接口设计 → 编码 → 联调 → 验收。不能因某个技术有趣就突然扩展成另一个项目。
- 每课要讲代码位置、调用链和预期结果；不能跳过用户实践，一次生成整套应用后算教学完成。
- 前端按用户选择使用 vibe coding；Java/Python 保留理解、动手与验收环节。
- 参考阅读、自己的实现、运行验证分别记录；不把文档宣传、固定返回、静态检查当作自己的功能或性能成果。
- Android、多模态、完整订单等目前是后续待决定范围。推迟不等于已完成、已取消或已经实现全部参考功能。

### 需要调整路线时怎么记录

用户最新明确指示优先。小范围文件命名、排错与实现选择可在现有授权内推进；它们不自动改变项目范围。

如果要增删主要模块、重排主要阶段、更换核心技术或改变教学方式，先在聊天中说明原因、影响与新的建议。已有用户指示足够明确时直接更新；需要用户决定的范围保持待定，不自行当作已选定。确认的新决定同步修改本路线、当前状态和学习日志，留下日期与理由。不要为常规操作另加审批流程。

每个阶段的验收未通过时，保留未解决项；可以进行独立工作，但不把该阶段标成完成。用户还没有亲手操作或讲明白时，也不能记录“用户已掌握”。

| 日期 | 版本/变化 | 依据与边界 |
| --- | --- | --- |
| 2026-10-11 | 0.1：建立按实际开发的全流程路线 | 用户要求从零、手把手、按实际开发教学；具体选型与后续范围仍待决定 |
| 2026-10-11 | 补充恢复入口、路线变更规则和逐阶段进度表 | 用户要求避免长聊偏离路线；未改变主要功能顺序或开始应用编码 |
| 2026-10-11 | 用户采用 Boot 4.1.1，继续 Java 17 与 MyBatis-Plus | 原因/影响已在聊天说明、独立实测；本轮从实际 POM 确认采用，正式构建成功。功能阶段顺序未改，IDEA 漏洞复扫未验收 |

## 9. 全流程进度与收尾检查

状态只能填写“未开始 / 进行中 / 已验收 / 待决定”，并附实际证据。准备文档完成不等于应用开发完成。

| 项目 | 当前状态 | 证据/下一步 |
| --- | --- | --- |
| 环境与学习档案准备 | 进行中 | 本机盘点、JDK 17 + Maven 启动和 GitHub 连接已核实；数据库权限、Redis、模型配置等按阶段补齐 |
| 0：认识项目与约定 | 进行中 | 第一课已开始介绍服务分工；尚未验证用户理解 |
| 1：Java 服务起步 | 进行中 | 现有健康 HTTP 历史验证通过；用户已采用新版 POM，正式 Maven package 成功，独立探针健康断言通过；用户配置后的正式启动、理解反馈与 IDEA 漏洞复扫仍待验收 |
| 2a：商品查询基础 | 已验收 | 2026-10-12正式构建、详情7项、列表14项成功/9项非法参数、正式列表与详情数据库故障503通过，见verification/product-list-2026-10-12.txt；用户概念理解继续巩固 |
| 2b：商品管理CRUD | 已验收 | 2026-10-13正式21源码package、真实MySQL和当前JAR共64项HTTP/SQL检查通过；006授权仅products SELECT/INSERT/UPDATE，练习商品6逻辑删除，原五条不变。见PRODUCT_CRUD_ACCEPTANCE.md及verification两份JSON。测试管理员凭据仅进程覆盖，真实凭据演练/用户Security理解暂缓回补，不标已掌握 |
| 3：Python 与跨服务调用 | 已验收 | 2026-10-15当前Java32源码package成功；Python模拟22项、Java/模拟Python22项、现有服务及一次真实模型链5项共49通过。Java→Python→官方DeepSeek→Java真实200、校验/认证/CSRF/模拟故障/超时/健康回声回归通过，见MODEL_HTTP_ACCEPTANCE.md与verification/model-http-2026-10-15.json；生产容量/总超时预算与用户理解不当已完成 |
| 4：首个导购版本与 Web | 进行中 | 2026-10-17首版同域网页与真实单轮导购、商品卡片/详情已实现并浏览器验证；27 HTTP断言、桌面1366/手机390和320宽度、隔离错误恢复/文本安全/分页/防双击通过，见WEB_FIRST_VERSION_ACCEPTANCE.md。用户体验反馈/教学回顾与正式界面完善待继续，React/Vite未实现；不将单样例当普适准确率或完整项目完成 |
| 5：知识检索 / RAG | 最小网页链已验收 | 原Python70/Java78/3真实样例保留，2026-10-20新知识页+真实USB-C引用/防水不足两样例通过；仅虚构K1短文单次问答，扩充资料/聊天保存未做，见KNOWLEDGE_WEB_AND_REAL_DEMO_ACCEPTANCE.md |
| 6：身份、会话与流式 | 最小网页链已验收 | 原21业务/89HTTP及工作台检查保留，2026-10-20真实模型两轮消息保存/流/刷新恢复已演示，见KNOWLEDGE_WEB_AND_REAL_DEMO_ACCEPTANCE.md；样例不代表普适语义效果或用户已掌握全部原理 |
| 7：Agent 工具与购物车 | 最小购物车工具链已验收 | 原226后端/36工作台/48商城检查保留；2026-10-20真实模型意图→PENDING→人工确认→刷新历史→独立cart整链通过。本轮27实际链/25受控UI/4只读复核见KNOWLEDGE_WEB_AND_REAL_DEMO_ACCEPTANCE.md。LangGraph图编排、订单支付未实现；下一共享会话与部署 |
| 8：部署复现、展示与简历验收 | 首个本地演示版本已验收并归档 | 完整Linux六服务、单Java故障恢复、独立RedisCluster分片复制与故障提升、SQL隔离恢复已核；用户实际商品基线30串行mean9.89/P9514.94ms。README/运行手册/实测/演示/贡献草稿已保存。下一本人源码复盘；新机初始化仍手动，生产HA/公网发布/版本化回退未交付 |
| Android、多模态、完整订单等后续范围 | 待决定 | 保留范围，不按已完成处理；结合目标与主流程进度讨论 |
| 参考全文源码阅读与运行验证 | 进行中 | 文档全文已读；源码覆盖见 `READING_COVERAGE.md`；参考运行尚未验收 |

最后收尾时逐项核对：

- [ ] 自己的 Web → Java → Python → 商品/模型链路可实际运行，关键场景有记录。
- [ ] 依赖版本可复现；配置模板不包含真实密钥；数据库初始化与样例数据说明完整。
- [ ] 写明启动顺序、端口、停止方法、部署方式与常见错误，并实际照说明验证；是否上云由用户届时决定。
- [ ] 登录、数据归属和修改操作有实际权限与失败检查；模型/依赖故障不会被包装成正常成功。
- [ ] 源码、接口文档、数据库设计、README 和当前进度一致；重要提交/推送记录可查。
- [ ] 用户能讲清一个正常请求、一个失败请求、核心技术取舍及个人完成的内容。
- [ ] 简历功能、数据和性能结论均有自己的实现与验证证据；借鉴来源与许可说明保留。

这些检查在对应阶段逐步完成，不要求在第一课之前全部做完。

## 参考来源

- [原项目设计文档](https://github.com/EvanYao826/ShopAgent-X/blob/e45e59402206beb262d0fc8d007ee9b7a232b644/docs/%E8%AE%BE%E8%AE%A1%E6%96%87%E6%A1%A3.md)
- [原项目部署与体验指南](https://github.com/EvanYao826/ShopAgent-X/blob/e45e59402206beb262d0fc8d007ee9b7a232b644/docs/%E9%83%A8%E7%BD%B2%E4%B8%8E%E4%BD%93%E9%AA%8C%E6%8C%87%E5%8D%97.md)
- [原项目学习指南](https://github.com/EvanYao826/ShopAgent-X/blob/e45e59402206beb262d0fc8d007ee9b7a232b644/ShopAgent-X%E5%AD%A6%E4%B9%A0%E6%8C%87%E5%8D%97.md)
- [原项目新手友好食用指南](https://github.com/EvanYao826/ShopAgent-X/blob/e45e59402206beb262d0fc8d007ee9b7a232b644/%E6%96%B0%E6%89%8B%E5%8F%8B%E5%A5%BD%E9%A3%9F%E7%94%A8%E6%8C%87%E5%8D%97.md)

## 10. 2026-10-12确认的多实例与集群目标

用户询问Redis集群模拟和Nginx负载均衡，明确不想做纯单体，并选择“Java + Python独立服务，Java多实例配Nginx，并做Redis集群演练”。因此该目标已纳入路线0.2；没有授权或需要现在把商品/用户/购物车拆成独立Java微服务。

目标拓扑（待实现）：浏览器→Nginx→Java实例A/B；Java调用独立Python AI服务；业务事实持久化MySQL；Java实例共享Redis缓存/临时状态。Python如需Redis时也使用兼容集群的客户端，不能默认照搬参考单节点redis.Redis连接。

实施顺序与主路线协调：

1. 阶段2a完成查询；按用户新决定，阶段2b先补齐管理CRUD并验收，再在阶段3/4接通Java→Python→真实商品/模型。
2. 在出现明确业务需要时接入Redis单节点：商品缓存或阶段6共享的临时会话/鉴权状态，确定key/TTL/失效/故障行为；MySQL仍负责需持久保存的业务事实。
3. 首版业务链路稳定后，在阶段8用项目级Docker Compose部署Java两个实例与Nginx，所有状态放到共享存储，检查轮询分发和停一Java实例后的服务行为；SSE阶段实现后复核代理缓冲、超时与断流。
4. RedisCluster本地演练采用建议的3主3从六节点拓扑，展示hash slot分片、复制、停一主节点后的提升与读写恢复，记录切换时间和失败窗口。单机多容器是本地演练，不能描述成多物理机容灾或生产高可用验证。
5. 回归登录/会话/商品/购物车/流式在多实例下的表现，再更新部署说明与简历事实。没有测量就不写吞吐、提升倍数、零丢失等结论。

网络与运行准备：此前Docker服务端未能连接，进入部署阶段时重新核对，不把旧环境体检当作当前状态。RedisCluster客户端必须能访问节点公布的地址和端口，不能只映射一个入口端口；优先把应用与Redis节点放同一容器网络并公布内部可达地址，不把容器私网地址直接交给Windows宿主机客户端。实际方案以部署阶段验证为准。

数据与工具注意：RedisCluster多键命令/事务/Lua需同一hash slot，后续涉及多键时设计hash tag；它的复制非强一致，购物车事实与防重复依据持久化业务约束，不靠缓存作为唯一事实来源。

验收证据：各服务/实例真实启动与HTTP调用；Nginx请求分别到A/B的可观察记录；停A后对B的服务验证；Redis slots覆盖、复制关系、主故障提升和数据恢复；局限、失败窗口、复现步骤与真实指标。

原参考固定提交docker-compose只部署单节点redis:7-alpine，本轮没有找到RedisCluster/Nginx部署配置；以上是自己的扩展目标，尚未实现。

官方依据：
- https://redis.io/docs/latest/operate/oss_and_stack/management/scaling/
- https://nginx.org/en/docs/http/load_balancing.html
### 商品查询完成与完整CRUD的区别（2026-10-12澄清）

阶段2原定交付仅为详情/列表/分页/搜索，即CRUD的R；验收通过不代表新增、修改、删除接口已经实现。五条样例通过初始化SQL插入，不算应用商品新增接口。当前专用数据库账号只有SELECT权限。

第一版导购需要可靠读取真实商品，因此查询验收后可进入独立Python与模型阶段；实际开发按功能依赖和业务闭环推进，不要求先为每张表实现全部CRUD。

2026-10-12最新决定覆盖之前顺序：用户明确先完成商品管理CRUD，再Python。新增阶段2b，先请求协议/校验与管理员边界，再产品表必要迁移与最小写权限，然后CRUD接口、管理查询与集中验收；公开查询仍只读。此请求授权本项目CRUD必要准备，不授权改动其他业务库、重置密码或批量生成全应用。

后续会话/购物车等业务仍有各自的新增、查询、修改、删除需求；首个导购闭环是中间里程碑，不是整个项目完工。
### 2026-10-12路线0.3：商品管理CRUD前置

用户明确选择先把CRUD完成。顺序变为商品查询基础（已验收）→商品管理CRUD（当前）→Python跨服务→AI导购/Web→知识检索→完整用户会话/流式→购物车工具→多实例/集群部署与简历。

Python专用环境和依赖文件保留；本轮不继续Python编码/联调，也不删除环境。当前阶段先完成商品管理接口；前端管理页仍可使用vibe coding，在接口稳定后补齐。Java/Python独立服务、Nginx和RedisCluster目标不变。

当前教学入口SECURITY_ERROR_RESPONSE_STEP.md：应用户进度请求只读确认新增POST/请求校验/Basic配置已写入正式源码，仍未新增运行验收。当前给完整securityFilterChain增量替换，统一401/403 JSON，用户手写；下一步CSRF令牌获取/携带，再集中核对正式DTO/实体/鉴权/非法请求，边界通过后授本表最小INSERT/UPDATE并联调新增练习商品。每几个相关步骤或功能节点集中检查，遇实际错误及时处理。完整CRUD验收前不能称阶段2b完成。

### 2026-10-12手机版预习展开（不改变路线0.3）

用户要求从上述当前课开始，加后续两个阶段的手机PDF，数量合并三册，风格参考《Spring Cloud work》中的17页基础手机稿。当前阅读入口改为docs/mobile/README.md及其三册；正式实践仍从尚未验收的CSRF/新增节点恢复，不能因PDF完成跳到Python。

第一册展开阶段2b完整商品管理CRUD；第二册展开阶段3健康/回声跨服务里程碑；第三册前半衔接阶段3真实模型配置与验证，后半阶段4首版导购与Web。健康/回声成功不等于阶段3真实模型验收。模型账号/供应商/预算未确定。

阶段3/4课程候选采用Java读取MySQL商品，Python先提取需求再解释Java候选，Java核对推荐编号与组装真实卡片。协议候选同步API_CONTRACT.md；最小静态Web实验台用于同域联调，正式界面仍按用户vibe coding方式推进。阶段5/6/7/8与多实例/集群目标保持，不在手机材料中提前记为完成。

教材源码只在docs/mobile/code及独立验证副本，正式应用未代写。三阶段Java编译与58项临时H2/本机Python/模拟模型验证通过；正式MySQL写CRUD、真实模型与页面仍需回电脑逐章实现和验收。

### 2026-10-13阶段2b局部教学调整

用户明确暂缓Security/认证/cookie/CSRF小节，后续回补；按ADMIN_PRODUCT_DETAIL_STEP.md继续管理详情、管理分页和修改/上下架/删除业务编码。保留现有权限和CSRF，不通过关闭安全检查继续开发。暂缓内容、安全HTTP验收、数据库最小写权限与新增201仍是阶段2b验收待办；几个小步后集中检查，CRUD全部过关后再Python。不是取消安全模块或改变架构目标。
