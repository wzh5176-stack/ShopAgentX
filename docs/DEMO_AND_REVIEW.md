# ShopAgent 演示与复盘

本轮首版交付归档：2026-10-22。项目类型是商品选购与购物车业务结合AI导购、资料问答，不包含订单支付交易闭环。

## 首轮复习的固定范围（2026-10-22）

用户问复习流程有多少，固定为五个专题，不把一个专题内的小步骤继续追加成新阶段。这是已实现代码的复习清单，不替代DEVELOPMENT_PLAN.md开发路线，不默认开发新功能。

| 专题 | 内容 | 当前讲解状态 |
| --- | --- | --- |
| 1. AI导购主链 | 入口、身份和历史、意图、真实商品、流式回复、事务保存与恢复 | 六步已讲完 |
| 2. 购物车 | 普通数量设置、AI待确认单、数量基线、重复确认、事务 | 本组用两个场景及事务说明收尾 |
| 3. RAG资料问答 | 资料分块、向量检索、携带证据回答、引用及依据不足 | 下一专题 |
| 4. 部署与项目表达 | 容器与Nginx双实例、共享会话、独立Cluster、备份恢复、实测边界、简历与演示 | 待复习 |
| 5. 登录与写请求保护 | Cookie/Session/Security/CSRF的实际请求用法 | 保留用户暂缓，最后单独讲 |

专题1的“六步”已经包含在第一专题，不是后面还要重做六阶段。每个剩余专题通常分一至两组，不承诺按聊天条数衡量进度。已讲完不等于确认用户掌握；用户主动提问可回看。五专题后按本文演示清单独立完整演示一次，结束首轮复习；不默认增加理论专题或新业务。

### 购物车收尾：两个场景与一个事务

1. 操作创建时数量1，目标3；等待确认时用户在另一页改为5。confirm锁定操作后读取当前数量，与baseQuantity=1不同，提交CONFLICTED状态并返回业务冲突；购物车保持5。比较的是当前数量值，不是所有历史修改的完整版本记录。
2. 创建时1、目标3，第一次确认成功保存3/SUCCEEDED；后来手动改为5，再确认旧操作。confirm先锁操作，发现不是PENDING，直接返回原操作结果3，不调用CartService，实际购物车保持5。返回的历史结果不等于实时购物车查询结果。
3. confirm的外层Transactional包含CartService设置数量与finishAction写SUCCEEDED；正常情况下二者一起提交，异常向外抛出则一起回滚。业务冲突则按设计保存CONFLICTED等终态，不等同于意外异常回滚。确认不调用模型。

本轮只读源码教学、仅更新复习记录；未执行示例请求/SQL、启动服务、调用模型或修改应用。此复习清单为首次Git上传后的本地文档更新，未自动提交推送。

## 演示前

按[RUNTIME_RUNBOOK.md](RUNTIME_RUNBOOK.md)启动网站六服务和Windows8080转发，检查healthUP。使用自己已注册的普通账号，保留同一个浏览器会话。Cluster/恢复实验平时停止，不必为网页演示全部启动。不展示密码、Cookie、密钥、账号散列或私人聊天内容。

## 一轮演示顺序

1. 商城首页→商品列表→关键词与商品详情。解释列表来自MySQL，示意图不是商品实拍。
2. 普通账号登录，进入AI导购新对话，发送“想买500元以内的跑鞋，用来日常跑步”。等待完整结果，指出商品名称/价格来自数据库候选。
3. 同对话发送“预算改成三百元呢？”。说明这次更新预算并保留品类，观察逐段输出；完成后刷新，展示历史恢复。
4. 选择推荐商品“设置购物车”，生成最终总数量操作。先展示PENDING卡，确认商品/数量再点击确认；打开独立购物车并刷新聊天卡片，展示实际结果。操作会改变演示账号数据，按需要恢复演示前数量。
5. 资料问答输入“演示耳机K1的充电接口是什么？支持无线充电吗？”。展开引用，说明USB-C/不支持无线充电来自虚构教学资料，不混成真实在售商品参数。
6. 展示[RedisCluster验收](REDIS_CLUSTER_ACCEPTANCE.md)、[备份恢复](MYSQL_RESTORE_CHECK_ACCEPTANCE.md)和[接口实测](PRODUCT_BASELINE_REPORT.md)。如需现场重做故障，应另按对应手动步骤恢复节点，不把停机操作混在正常演示中。

本轮部署后的真实网页链与Java故障窗口为用户实际反馈，恢复状态/集群数据/恢复库检查另有助手只读证据，不能声称助手独立观察了所有浏览器行为。

## 三条应能讲清的请求路径

### 真实商品导购

浏览器POST本人会话消息流→Java检查登录和会话归属、加载上下文→Python调用模型提取/更新需求→Java按条件查询商品→Python选择候选并生成解释流→Java复核/转发并保存消息→网页展示商品卡和历史。模型输出不能直接当商品事实或可信用户身份。

### 人工确认购物车

网页明确引用某次推荐→Java核当前用户与来源消息→Python识别设置数量意图→Java复核商品、保存PENDING操作和消息→用户确认→Java短事务锁定操作/相关数据，检查有效期与数量基线→更新购物车并保存成功状态。重复成功确认回放结果，不再次执行；取消/过期/冲突按状态返回。数量是最终总数量，不是默认增量。

### 双实例共享登录

浏览器Cookie携带会话标识→Nginx分发到A/B→Spring Session从共享Redis读取登录会话→Spring Security执行访问控制→Java按可信身份查MySQL。Redis里共享的是会话，业务永久数据仍在MySQL；Nginx自身不负责登录验证。

## 常见追问与回答依据

| 追问 | 应说明 |
| --- | --- |
| 为什么Java与Python分开？ | Java管理权限/事务/持久化，Python处理模型与向量检索，通过HTTP协作 |
| 怎么减少模型乱编商品？ | 真实候选与ID校验、结构化输出、Java复核；不能承诺模型永不出错 |
| 为什么先PENDING再确认？ | 自然语言理解不能直接等同用户批准业务修改 |
| 为什么要幂等和数量基线？ | 防重复点击重复执行，识别生成操作后购物车已被别处修改 |
| SSE什么时候算保存成功？ | 区分正在输出与完成/落库，历史恢复读取服务端记录 |
| 两Java能否各存登录？ | 实例内存会话不共享，当前用Redis集中保存HttpSession |
| Cluster与网站的Redis关系？ | Cluster为独立三主三从演练，网站仍单节点共享会话Redis |
| 原主恢复为什么变副本？ | 新主已接管槽，原主跟随新主同步，未强制抢回角色 |
| 101.08是不是最大QPS？ | 不是，只是30次单并发小样本实际速率 |
| 备份文件存在就能恢复？ | 不够；已在独立无网络MySQL中恢复六表并核结构、引用 |
| 和完整电商还有什么距离？ | 订单、支付、地址物流和运营等未实现 |
| 用了LangGraph吗？ | 当前未接LangGraph图编排，已有业务分流与工具操作链 |

## 学习复盘顺序

按1→6顺序阅读完整讲解：[一次导购请求的六步完整流程](GUIDE_REQUEST_SIX_STEPS.md)。下面逐课短记录保留上下文，不必从倒序记录开始复习。

### 本轮导购主线固定六步

| 步骤 | 内容 |
| --- | --- |
| 1 | 网页请求与Controller入口，URL/正文/可信身份 |
| 2 | 会话归属、最近10条历史与消息版本 |
| 3 | Java调用Python，结合历史提取关键词/预算/澄清问题 |
| 4 | Java查真实候选，Python选择，Java复核商品ID |
| 5 | Python解释流→Java SSE→网页逐段显示 |
| 6 | 短事务保存消息、版本冲突、done与历史恢复 |

2026-10-22六步导购主链已逐步讲到第6步，非确认用户全部掌握。这六步是一次正常导购请求的主链，不是整个项目只含六模块；澄清/无商品分支可提前形成答复。购物车、RAG、Cookie/CSRF与部署是独立复盘主题，后续按需要与暂缓记录继续。

### 2026-10-22第六课：短事务、版本检查、成对保存与历史

只读实际ChatMessageWriteService.saveTurn、ChatMessageStreamService.runTurn/StreamJob.success、MessageSendResponse/MessageResponse、历史Controller与workspace.js。模型和网络生成在saveTurn事务外，外层调用独立Service的Transactional方法，正常经Spring事务代理提交后才回到调用者job.success。saveTurn按本人会话查询FOR UPDATE串行化相同加锁流程，再用最新消息id的LIMIT1 FOR SHARE当前读比较expectedLastMessageId；若不一致抛RuntimeException ConversationChangedException，防旧历史结果混入新会话进展。示意两请求都看50，A先存51/52，B看到52与期望50不一致而拒绝本轮写入；不以示意ID当真实数据。

事务中插入user(content/recommendation_json null)、assistant(完整answer/recommendation_json)两条消息，更新会话updated_at，返回MessageSendResponse(userMessage,assistantMessage,recommendation)。这里的保存失败/冲突运行时异常触发回滚，两条成对提交，不把外部模型等待包在这个事务内。

提交后success发业务done(saved)并complete，网页验证/merge两消息、重绘并标已保存；SSE已开始后的冲突用error事件code CONVERSATION_CHANGED，不称服务器此时重设HTTP409（网页内部映射409）。若提交后接收端断开，已提交消息仍在历史，不承诺停止显示能撤销提交。刷新/选择对话GET /api/conversations/{id}/messages?size=20，服务归属检查后读MySQL并解recommendation_json恢复商品卡；模型最近10条不删除数据库旧历史，网页20条是独立分页用途。无真实请求/事务/模型执行或应用代码变动。

六步贯通：入口→归属历史→意图→候选选择→流预览→版本事务保存/业务done与历史。后续按用户选择独立复盘购物车状态机、RAG或保留的Cookie/CSRF主题，不无限增加本轮步骤。

### 2026-10-22第五课：解释流与网页逐段显示

只读实际Python explanation_service/main/model_stream_client/stream_events、Java AiClient/AssistantStreamService/ChatMessageStreamService、workspace.js。选定商品与合并条件POST /api/recommendations/explain/stream，prompt仅解释已选事实，model payload streamTrue。httpx逐行解析模型content生成器yield；stream_events包装event:delta/data:{content}/空行，经StreamingResponse text/event-stream送Java。

Java readExplanation收到delta：StringBuilder追加并onDelta.accept；实际回调job::delta在StreamJob通过SseEmitter发给浏览器。workspace.js用reader/TextDecoder分帧，JSON.parse后answer+=value.content并onDelta(answer)，外层回调target.textContent=answer，形成逐段预览。网络块不等于事件/字符，实际代码保留pending跨块解析；本课无需深入协议解析实现。

必须区分两条done：Python→Java的done只表示解释完整；AssistantStreamService校验累计内容后返回最终结果，不直接透传该done给浏览器。Java→网页的业务done在saveTurn提交后job.success(saved)才发送；网页validPair/mergeMessages后才显示已保存。error不是成功done；连接结束缺done不能当成功。第6步再讲短事务/消息版本、两消息成对保存和刷新历史。无真实模型/HTTP调用/应用改动。

### 2026-10-22第四课：真实候选与模型选择

只读实际AssistantService.searchCandidates(AiIntentData)、ProductService.findCandidates、AssistantStreamService.recommend、AiClient.selectProducts、Python selection_service与schema。本轮意图已提取，流路径使用searchCandidates(intent)重载，不重复调用searchCandidates(question)再提意图。预算字符串转BigDecimal；按上架status1、标题或品牌LIKE、可选price<=预算、@TableLogic自动deleted0、id升序取首20个，返回List<Product>。示例跑鞋300预算仅说明条件，不在本课执行SQL。

候选转AiCandidateProduct给Python，同时Map<Long,Product>保留id→原商品。Java POST /api/recommendations/select传question/products/searchIntent（合并历史条件，当前问题可能省略品类）。Python模型只选最多5个recommendedIds、不生成理由，严格整数数组并检查候选成员、去重；Java也检查候选ID及数量，再从Map还原原Product，名称/价格来自查询候选。没有候选或选择空数组走提示，不编商品。候选筛选最多20个，不是全库任意生成或商品向量检索；RAG资料为独立链。

下一第5步readExplanation→AiClient.streamRecommendation→Python解释流→SSE delta与网页累加；第6步再看saveTurn/版本与done，避免把文字正在显示当已经落库。无模型/DB调用或应用更改。

### 2026-10-22第三课：Java与Python协作识别需求

只读实际AiClient.parseConversationIntent、Python main.py/conversation_service.py/conversation_schema.py与返回DTO。Java将AiConversationIntentRequest(question,history)序列化JSON，POST /api/conversations/intent；当前容器基址http://ai:8000。Python FastAPI校验请求→extract_conversation_intent组规则+历史/当前问题prompt→ask_model(json_mode=True)→ConversationIntent.model_validate_json→normalize_intent与澄清整理→data包装。Java解成AiConversationIntentResponse，再取AiConversationIntentData并复核格式。示例历史500跑鞋、本轮改300→keyword跑鞋/maxPrice300.00/clarificationnull；规则要求新预算优先、明确品类可继承、模糊便宜点需追问，均为期望行为而非承诺模型永远遵守。没有实际模型/HTTP调用。

下一第4步沿AssistantStreamService.recommend→AssistantService.searchCandidates和ProductService候选查询，再selectProducts与候选ID复核；本课没讲成Python直接查商品DB。

### 2026-10-22第二课：会话归属和模型历史

只读实际ChatMessageService.loadContext、ChatConversationService.getOwned、CurrentUserService.requireUserId及两个DTO。getOwned从可信Authentication用户名查有效AppUser.id，再同时按会话id和user_id查询；失败抛同一ConversationNotFoundException，不继续读别人消息。loadContext只选消息id/role/content，按conversation_id、id倒序取第1页最多10条，false不查count；先记倒序首条ID为lastMessageId（空历史0），再reverse按旧→新构造role/content列表，List.copyOf后与版本一起返回ConversationContext。最多10条消息，不是10轮/全部历史。

当前问题未混进这份既有历史；runTurn将content与context.history分别构造AiConversationIntentRequest(question,history)发给Python识别本轮需求。lastMessageId只留Java后续saveTurn做并发版本检查，不放进Python请求，也不是消息条数。示例500跑鞋→预算300仅说明JSON形状，无真实请求/DB查询/模型调用或应用更改。下一沿AiClient.parseConversationIntent→Python /api/conversations/intent 与conversation_service解释历史如何参与识别，以及Java怎样用keyword/maxPrice查商品。

### 2026-10-22第一课：真实导购请求入口

只读源码，无模型请求/服务启停/代码改动。当前聊选购网页workspace.js的streamGuide发送POST /api/conversations/{conversationId}/messages/stream，JSON为content；Java ChatMessageStreamController.stream接收路径会话ID、MessageSendRequest与Spring提供的Authentication。MessageSendRequest为record content，NotBlank/Size1000配合Valid校验。Controller调用ChatMessageStreamService.stream后返回SseEmitter/text-event-stream通道，后续delta与done由服务发送，不是Controller本身生成答案。身份不是浏览器JSON传入的userId。

本课先讲URL/JSON/身份三种来源和Controller/DTO/Service职责。下一看ChatMessageStreamService中的chatMessageService.loadContext(authentication, conversationId)，解释会话归属检查与历史/版本读取，再追AiClient.parseConversationIntent；暂缓Cookie/CSRF主题未因此视为学完。示例会话ID只作解释，不执行请求。

上面的六步导购主链已讲完。后续以本文开头的五专题复习清单为准，不再追加导购第七步或新业务；购物车本组收尾，下一RAG，身份与CSRF仍保留最后暂缓安排。

此前用户暂缓的Cookie/认证/CSRF主题继续保留：用“谁生成、浏览器如何携带、哪个过滤器验证、Controller拿到谁”解释，结合当前SecurityConfig与网页请求；不直接从完整框架原理开始。手写学习保持解释→用户操作→成组验收。

代码入口可从[项目首页](../README.md)查目录，历史教学与验收保留，不将旧候选状态当最新结论。

