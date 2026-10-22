# ShopAgent 接口约定与阶段记录

2026-10-22当前状态：Spring Session已用于完整Linux双Java共享登录，Cookie为JSESSIONID，运行配置Redis namespace为shopagent:runtime:session；用户停A后身份/商品/cart反馈正常，助手另核恢复后六healthy。已有商城、本人会话多轮流、turns待确认/confirm/cancel、资料问答，当前入口列表见根README与实际Controller/SecurityConfig。下方按日期保留的“候选/待验”是历史记录，不能替代后续验收；本轮状态以RUNTIME_RUNBOOK.md、REDIS_CLUSTER_ACCEPTANCE.md、MYSQL_RESTORE_CHECK_ACCEPTANCE.md为准。没有订单支付或网站接入Cluster的新API。

2026-10-20会话共享接入教学候选（未实现/未验）：计划Spring Session把原HttpSession存到专用Redis，HTTP路径/登录表单/本人权限/CSRF规则不变，显式Cookie名JSESSIONID兼容原logout；Boot4.1属性spring.session.data.redis.namespace=shopagent:session，空闲timeout30m。迁移后旧内存登录不自动转移，需重新登录；跨Java会话与退出/故障语义待实际集中验收。见SESSION_REDIS_MANUAL_STEPS.md，不把候选当正式协议运行结果。

2026-10-20资料问答网页已接既有POST /api/assistant/knowledge：匿名可用但保留CSRF，JSON question为1～1000字符，data含answer/answerable/sources；answerable=true需1～3真实引用，false必须空引用。引用chunkId/source/document/section/content均纯文本显示，可展开核对，示例问题只填草稿。页面同源Cookie/新CSRF/35秒客户端等待，失败保留问题、不自动重试，不将此单次问答写入私人会话。真实模型2知识问答样例、真实商品会话和工具确认已运行；新页面不提供新后端接口，见KNOWLEDGE_WEB_AND_REAL_DEMO_ACCEPTANCE.md。

2026-10-20新增商城网页映射，无新增后端API：home/products用GET /api/products（当前分类复用keyword，不是数据库分类）；product.html?id=读取GET /api/products/{id}；cart.html用本人GET /api/cart及PUT/DELETE /api/cart/items/{id}。详情输入明示最终总数量，已有数量回填；不伪装增量ADD。不可用cart条目brand/price允许null，仍可移除，合计按后端返回。play.html?question=仅填草稿，由用户手动发送。index默认home、AI工作台保留，登录Cookie/CSRF/XHR401规则保持。订单支付接口未实现。正式48技术+最终18只读检查见STOREFRONT_ACCEPTANCE.md。

2026-10-20网页兼容约定已运行验证：网页fetch携带X-Requested-With: XMLHttpRequest，匿名401仍JSON但不发WWW-Authenticate，避免浏览器原生Basic弹窗锁住页面。普通匿名请求保留Basic挑战、有效Basic仍通过；此头不认证、不代替CSRF，原精确USER/ADMIN规则不变。网页普通导购用messages/stream，购物车模式带真实sourceMessageId用turns；只有done或成功响应视作已保存。历史卡片由cartOperationId读本人操作GET最新状态；confirm/cancel不由网页重传数量，409使用返回data当前状态。见WEB_WORKSPACE_ACCEPTANCE.md与36项报告。

2026-10-19正式合同更新：016消息关联、MessageResponse七字段和ConversationTurnResponse六字段已由正式131源码/226技术检查验收，见TOOL_MESSAGE_ACCEPTANCE.md。clarify会保存TEXT消息对，cart_action会原子保存PENDING和TEXT用户/CART_ACTION助手消息，响应顶层userMessage/assistantMessage非null；guide仍原guide内消息、顶层新增二字段null。history返回messageType/cartOperationId，操作最新状态沿原本人GET；工具并发会话变化409，同一失败事务不留操作或消息。本条覆盖此前工具响应/保存草案，网页和真实模型语义未验。

2026-10-19工具保存新草案（未验收）：turns响应在原四字段后追加userMessage和assistantMessage；工具clarify与cart_action返回已保存的消息对，guide仍读取guide.userMessage/assistantMessage，顶层两字段null。clarify从旧只返回提示改为同短事务保存TEXT消息对；cart_action同事务创建操作并保存TEXT用户+CART_ACTION助手关联；会话版本冲突沿409 CONVERSATION_CHANGED，创建仍不执行cart。无新路径安全变更，历史七字段/操作GET最新状态合同保持。见TOOL_MESSAGE_WRITE_MANUAL_STEPS.md。

2026-10-19工具历史响应草案：MessageResponse在id/role/content/createdAt/recommendation之后追加messageType和cartOperationId。TEXT/null为普通消息，CART_ACTION/UUID为操作提示，状态仍取原本人操作GET；history data.items与原普通/流式保存后消息响应复用此DTO。旧字段保留，三个构造调用同步迁移。本组手写教案未运行，工具分支实际保存尚待，示例未来结构不作运行证据。见TOOL_MESSAGE_RESPONSE_MANUAL_STEPS.md。

2026-10-19工具历史存储草案：chat_messages拟增message_type TEXT/CART_ACTION、cart_operation_id。TEXT涵盖普通用户/助手/澄清及已有推荐；CART_ACTION仅assistant确认提示、无推荐JSON、关联真实操作。每operation至多一条提示，普通null可多条；同用户同会话仍服务端检查。历史响应尚未新增字段、工具写入与卡片恢复未接，不改已验收turns合同。未来历史回操作ID后调用原本人GET取最新状态。详TOOL_MESSAGE_STORAGE_MANUAL_STEPS.md，本轮未创建/执行正式迁移。

2026-10-19正式状态：POST /api/conversations/{conversationId}/turns两组手写代码已验收，130源码构建/128技术检查通过，覆盖前述草案待验状态。USER+Cookie/CSRF，正常200 data包guide/clarify/cart_action；新操作PENDING只保存，不执行cart；正常confirm/cancel沿既有接口。guide保存消息，工具两分支还未保存chat_messages或恢复卡片；旧messages/stream仍仅导购。本轮模型受控非真实语义，详CONVERSATION_TURN_ACCEPTANCE.md。下一先设计工具消息/操作关联及会话版本保存边界。

2026-10-19统一交互入口第二组草案：CONVERSATION_TURN_HTTP_MANUAL_STEPS.md给POST /api/conversations/{conversationId}/turns Controller及USER规则，正常guide/clarify/cart_action统一200 data=ConversationTurnResponse，独立cart-actions创建仍201 Location。已登录会话Cookie+有效CSRF头，字段/来源/归属错误沿既有400/404，安全错误沿401/403；旧messages/stream不变。本轮仅手写教案，未运行，不能记接口通过。工具回复历史/卡片恢复后续，sourceMessageId取有商品的本人同会话推荐assistant消息ID。

2026-10-19会话分流教案草案（尚未实施）：拟POST /api/conversations/{conversationId}/turns，USER+CSRF，正文content与可选正数sourceMessageId（服务端已存的本人同会话有商品assistant推荐）。响应拟data={type:guide|clarify|cart_action,guide:原MessageSendResponse或null,cartAction:原CartActionResponse或null,message:展示文字}。无来源走旧导购；带来源prepare none回导购、clarify返回澄清、set_quantity调用create保存PENDING。新入口本版非流式，旧messages/stream不变；工具分支本课未写chat_messages，不能称已保存完整工具对话。当前仅三文件手写教案，Controller/Security待下一组，正式源码与HTTP尚未验收。见CONVERSATION_TURN_ROUTING_MANUAL_STEPS.md。

2026-10-19正式状态更新：此前创建/读取/确认/取消候选合同已由实际保存代码、真实MySQL和正式JAR验收，154项技术检查通过，见CART_ACTION_ACCEPTANCE.md。接口路径及200/201/409与USER+CSRF约定保持；创建只保存PENDING，确认原子写cart和结果，成功重试只回放，取消不改cart。GET /prepare受操作ID校验返回400，与旧阶段预期403有别。会话自动工具路由、前端确认卡片和真实模型语义仍待，不把独立接口通过等同完整Agent完成。

2026-10-19确认/取消候选合同补齐（手写待验）：POST /api/conversations/{conversationId}/cart-actions/{operationId}/confirm和/cancel，USER+CSRF，不读取新的商品数量正文。成功及成功重复confirm为200 data=SUCCEEDED/历史result，成功及重复cancel为200 data=CANCELLED/result null；其他不能执行的终态409同时包含data当前状态和error(code,message)：EXPIRED→CART_ACTION_EXPIRED、CONFLICTED→CART_ACTION_CONFLICTED、REJECTED→PRODUCT_UNAVAILABLE、取消后确认→CART_ACTION_CANCELLED、成功后取消→CART_ACTION_ALREADY_SUCCEEDED。确认锁操作/商品/cart当前读并复核截止与数量基线；cart写入与SUCCEEDED结果短事务原子提交。业务拒绝先提交终态再Controller组409，真实技术异常整笔回滚。成功重试只回放，不因当前cart/商品/到期变化重新执行。详CART_ACTION_DECISION_MANUAL_STEPS.md，实际表及全链待整组验收，无新运行结论。

2026-10-19操作创建/读取候选合同（用户手写待验）：POST /api/conversations/{conversationId}/cart-actions接既有CartActionCreateRequest，验证本人同来源真实推荐和当前商品可售，读取本人cart数量基线，生成UUID/PENDING/服务器Clock+10分钟，事务内只INSERT cart_actions回读，201 Location/data=CartActionResponse(result=null)。GET末尾/{operationId}按本人+会话+规范小写UUID读200，同归属范围找不到404 CART_ACTION_NOT_FOUND；父会话沿404。读取只将仍PENDING且已到期的响应状态投影EXPIRED，不修改DB；成功历史不被到期覆盖。USER且POST保留CSRF，旧prepare保留；新GET通配下GET /prepare会进入UUID校验400。创建重复会生成不同编号，本组不承诺创建幂等，也未实现确认/取消。代码见CART_ACTION_CREATE_READ_MANUAL_STEPS.md，实际014/015与本组待后续统一DB/HTTP验收。

2026-10-19操作计划存储候选（尚未执行/实现）：cart_actions用Java生成operation_id字符串PK，记录user_id/conversation_id/source_message_id/product_id/title/base_quantity0～99/quantity1～99/status/expires_at/executed_quantity/created_at/updated_at；六状态同CartActionStatus，只有SUCCEEDED时executed_quantity非null且等于目标，其他状态null。四外键仅保证引用存在，归属/真实推荐继续Java复核；取消更新状态，不物理删，应用只新表SELECT/INSERT/UPDATE。见CART_ACTION_STORAGE_MANUAL_STEPS.md，当前只建表和映射手写教案，无创建/确认接口或DB执行，UUID唯一键并不自动构成完整幂等。

2026-10-19正式更新：Python POST /api/cart/intent与Java POST /api/conversations/{conversationId}/cart-actions/prepare已正式技术验收，120Java构建/Python51+Java和整链102全部通过，见CART_TOOL_WIRING_ACCEPTANCE.md。下方同名“候选/未验收”段落保留历史，当前以此为准；请求/响应字段和错误合同未改。真实模型语义与前端待验，prepare无持久化计划/无cart写入；确认取消接口仍待实现。

2026-10-19 Java准备接口候选（未实现/未验收）：POST /api/conversations/{conversationId}/cart-actions/prepare，USER+CSRF，正文CartToolPrepareRequest(sourceMessageId正整数,question非空白≤1000)，不接客户端商品列表/userId。Java读本人同会话assistant保存推荐的原顺序，给Python仅title/brand/price，消费action/index/quantity并独立复核后映射真实id、重查当前在售未删商品。200 data=CartToolPrepareResponse(action,conversationId,sourceMessageId,product或null,quantity或null,message)；无持久化计划、operationId或执行结果。输入400、归属会话404、来源消息不匹配统一404 RECOMMENDATION_SOURCE_NOT_FOUND、当前商品失效409、Python响应503、DB沿503、坏历史500。新增10Java类+原AiClient/Advice/Security增量见JAVA_CART_TOOL_WIRING_MANUAL_STEPS.md；用户要求整组写完后总体检查，本轮未运行任何验收，不把候选记正式完成。

2026-10-19 Python工具意图候选（未正式实现）：POST /api/cart/intent请求question(1～1000非空白)+products(1～5项title≤200/brand≤100/规范两位price字符串)，无商品id/身份参数，额外字段422。200 data含action(set_quantity/clarify/none)、productIndex(StrictInt 1～5或null)、quantity(StrictInt 1～99或null)，三字段必须有；set_quantity两参数非null且位置≤实际列表长度，其他action参数均null。输入校验422且不调用模型，上游/格式/关系不合法503 AI_SERVICE_UNAVAILABLE。位置按同一来源消息的原推荐列表从1起映射，Java后续独立复核，不是数据库id，不直接写购物车；增量/不支持操作或缺目标信息clarify、普通咨询none。仅tmp候选51技术预检，无真模型语义与正式HTTP验收；详CART_TOOL_INTENT_MANUAL_STEPS.md。

2026-10-19最新购物车修正：正式107源码构建/98项全通过，JSON数量小数和字符串已400，普通购物车通过，不再沿用下方旧待修状态。

2026-10-19待确认操作候选合同（未实现）：POST /api/conversations/{conversationId}/cart-actions接CartActionCreateRequest(sourceMessageId/productId/quantity)，验证本人会话/assistant来源消息及已存真实推荐/当前可售后创建计划，201 Location/data，不改cart。GET同路径/{operationId}本人状态；POST末尾/confirm或/cancel无修改目标正文，USER且POST保留CSRF。CartActionResponse含operationId(服务器生成)、conversationId、sourceMessageId、productId、title、baseQuantity(无条目0)、quantity(目标1～99)、status、expiresAt(拟10分钟、服务端时间)、result(仅成功CartItemSetResponse，其余null)。状态PENDING/SUCCEEDED/CANCELLED/EXPIRED/CONFLICTED/REJECTED；确认基线变化/过期或失效拒绝，成功后重复确认只回放原结果不重新修改cart；状态与cart结果短事务原子保存。具体错误与事务合同见CART_ACTION_DTO_MANUAL_STEPS.md，尚无正式DTO/表/接口/Agent实现。

2026-10-19购物车正式集中验收更新：HTTP路由已存在且已运行，98项96通过，2项quantity格式转换待修。合同要求JSON整数1～99；小数与字符串应400，目前正式1.5→1、字符串2→2均200，详CART_ACCEPTANCE.md。WebJsonConfig隔离修正候选98/98，用户手写后待正式复验，不降低合同为允许自动转换。其余下述正常响应、USER/CSRF、本人隔离、重复数量/并发及金额行为已按报告范围验证。

2026-10-19购物车HTTP候选合同补齐：PUT /api/cart/items/{productId}首次/更新均200，data=CartItemSetResponse(productId,quantity)，来自成功保存后的回读；请求仅quantity，设置绝对数量，不累加，不暴露userId/数据库行。GET /api/cart 200 data=CartResponse；DELETE同items路径204无正文，即使条目不存在亦正常。三路由ROLE_USER，PUT/DELETE保留CSRF，Controller/安全规则教案见CART_CONTROLLER_MANUAL_STEPS.md，尚未正式运行验收。非法编号/数量400，商品不可用409 PRODUCT_UNAVAILABLE；认证/角色/CSRF沿既有规则。

2026-10-19购物车读取候选合同完整化：GET /api/cart 200 data=CartResponse(items,totalQuantity,totalAmount)，每行CartItemResponse(productId,title,brand,price,imageUrl,quantity,available,subtotal)，无userId；按cart id升序。totalQuantity含失效行件数，totalAmount只合计可用行；金额均两位字符串。下架行可见真实商品字段、available=false/subtotal=0.00；逻辑删除商品被MP过滤后保留占位行（productId/quantity，标题商品已删除或不存在，brand/price/imageUrl可null，available=false/subtotal=0.00）。空车items=[]/0/0.00。当前仅CART_READ_MANUAL_STEPS.md手写教案与tmp编译，不宣称GET已存在；写接口响应DTO/Controller下一组补齐。

2026-10-19购物车写业务候选新增：商品不存在/已下架/逻辑删除时，CartProductUnavailableException经Advice转409 PRODUCT_UNAVAILABLE（固定刷新文案）。quantity及productId非法400；身份失效沿401。setQuantity只接Authentication/productId/quantity，从认证取userId，事务内在售商品FOR SHARE→upsert→本人条目FOR SHARE回读。remove不检查商品在售，允许清除失效条目；不存在目标正常，未来Controller204。当前只有完整教学候选，无正式CartService/Controller/HTTP验收；见CART_SERVICE_MANUAL_STEPS.md。

2026-10-19购物车候选输入进一步明确：CartItemSetRequest仅Integer quantity，非null且1～99；未来PUT /api/cart/items/{productId}设置绝对总量，路径给productId，登录身份给userId。Mapper候选setQuantity(userId,productId,quantity)用INSERT ... ON DUPLICATE KEY UPDATE quantity=#{quantity}，重复请求不累加；它不等同工具确认只执行一次的完整幂等机制。无新增正式Cart HTTP/Service。只读已看到cart_items和四权限；当前完整三文件手写见CART_MAPPING_AND_REQUEST_MANUAL_STEPS.md。

2026-10-19阶段7候选协议（未实现）：GET /api/cart读本人清单；PUT /api/cart/items/{productId}正文quantity整数1～99设置目标总数量，重复不累加；DELETE同路径移除本人清单行，不删商品，不存在204。ROLE_USER/修改CSRF/身份从CurrentUserService取，输入不能指定所有者或价格。购物车查询将含实时商品事实/available状态及总量金额，失效商品保留供移除且不计可用金额；完整DTO/错误合同在Controller前补齐。表cart_items id/user_id/product_id/quantity/created_at/updated_at，唯一用户商品组合，两外键及数量CHECK；012/013脚本未执行，CartItem/Mapper手写待。工具计划绑定本人/会话/真实推荐/数量基线/有效期与操作编号，确认再执行/取消不修改，后续单独设计，不让模型直写DB。详CART_FOUNDATION_MANUAL_STEPS.md。

2026-10-19正式新增POST /api/conversations/{conversationId}/messages/stream，MessageSendRequest(content非空≤1000)，USER+同会话CSRF+本人所有权。开流前400/401/403/404/饱和503 JSON；开流后delta=AiStreamDelta内容预览，唯一终态done=MessageSendResponse（实际userMessage/assistantMessage/recommendation，saveTurn提交后发送），error=AiStreamError(code,message固定安全文案)。Python模型done仅内部完整校验，不转发为业务完成。冲突开流后error.code=CONVERSATION_CHANGED，非流式旧接口仍409。提交前取消不保存；进入短事务提交阶段后接收端断开不撤销已开始的事务，应查询历史确认，不能据未收done自动重发。合同与89真实HTTP/DB技术检查详CONVERSATION_STREAM_ACCEPTANCE.md；真实模型效果及网页接线待验。

2026-10-19最新覆盖前课待验：Python POST /api/recommendations/explain/stream与Java选择/解释客户端用户已写并通过81针对性技术检查，见RECOMMENDATION_EXPLANATION_ACCEPTANCE.md；无真实模型或新会话流HTTP。本课AssistantStreamService.recommend(question,intent,onDelta,onOpen)仅内部业务候选：条件查询→选id→实际商品映射→已选事实理由流，onDelta仅传验证后文字，Python done内部核对后返回AssistantResult，不向调用端标记落库完成。空候选/空选择不调用说明模型，返回固定answer/空商品（onDelta可零次）；失败无成功结果。未来私有会话层先验证本人、合并历史条件，再本方法，再saveTurn原版本短事务保存；本课不新增可绕过所有权的对外路由。

2026-10-19最新：POST /api/recommendations/select用户正式代码已通过32技术检查，受控模型/TestClient。新教学候选POST /api/recommendations/explain/stream接question/products/searchIntent：question沿AskRequest，products为已选1～5真实商品而非最多20全候选，searchIntent沿共享可选条件。输入422；准备条件非法流前JSON503；成功text/event-stream，delta.content与done.answer/service复用模型流协议；模型流中故障error事件，HTTP可200；Python done不意味着Java落库。空选择由未来Java业务直接响应，勿传空products。Java新增选择响应两DTO及selectProducts(AiAskRequest,AiIntentData)、streamRecommendation(AiAskRequest,AiIntentData,onEvent,onOpen)，两者要求非空规范合并条件；共享streamRequest保留旧streamChat两重载。均为本课待手写正式协议，不声称新会话入口/保存已实现。

2026-10-19正式管理员诊断流64技术检查通过，仍无商品或私有消息落库。当前新增Python选择接口草案POST /api/recommendations/select复用AskRequest(question/products1～20/searchIntent可选)，200 data={recommendedIds:严格整数列表≤5，空正常}；候选外编号/非法结果503，输入schema错误422。模型选择与解释流分开，Java必须复核选择/展示真实商品字段，完整流式路径3次模型阶段；本组仅教案和tmp32预检，正式路由待手写验收。详PRODUCT_SELECTION_MANUAL_STEPS.md。

2026-10-19新Java流式诊断教案POST /api/admin/ai/chat/stream，复用ChatRequest.message/ADMIN+CSRF；ResponseEntity<SseEmitter> text/event-stream，delta/data DTO、done/AiChatData、error/AiStreamError，已开流错误HTTP200内error；超时可能EOF无done，不标成功。流前400/队列满503明确application/json（原helper加contentType）。仅手写教案和副本30+兼容27，正式未新增HTTP/未验收；私有导购与保存后续。AiClient三参onOpen提供本请求InputStream以显式取消，旧两参兼容保留，详JAVA_MODEL_STREAM_RELAY_MANUAL_STEPS.md。

2026-10-19Java读取正式34技术检查通过，当前无新增Java HTTP流接口/放行。新教案AiStreamJsonCodec将delta.data→AiStreamDelta，done→已有AiChatData并严格匹配累计文字/service，error→AiStreamError并规范安全文案；AiStreamConfig有界线程池待Service使用。本组为用户手写草案及副本42预检，不标正式JSON/转发或落库完成。见JAVA_STREAM_READER_ACCEPTANCE.md/JAVA_STREAM_CODEC_EXECUTOR_MANUAL_STEPS.md。

2026-10-19Python POST /api/model/chat/stream正式技术69检查通过，delta/done/error及422、已开流HTTP200内error成立，真模型请求0待验；模型done不代表Java保存。当前Java教案AiStreamEvent/Reader/AiClient.streamChat仅内部消费同一路由，未新增正式Java HTTP接口/放行或持久化。Reader只校验帧与类型，data仍JSON字符串，严格业务字段/终结内容一致性后续再接。见PYTHON_STREAM_ACCEPTANCE.md与JAVA_STREAM_READER_MANUAL_STEPS.md。

2026-10-19当前流式教案草案：Python新增POST /api/model/chat/stream，请求{"message":"..."}复用ChatRequest非空白≤1000；200 text/event-stream，delta data={content}、done data={answer,service}、error data={code:AI_SERVICE_UNAVAILABLE,message}。前置422，已开流后的故障为error事件而非再改HTTP503，无error后done。此模型done不表示Java落库；私有导购流/候选/会话保存尚未接。仅用户手写教案与临时副本30预检，无正式路由验收，见STREAM_HTTP_MANUAL_STEPS.md。

2026-10-19正式多轮技术修正后98项通过，消息FOR SHARE/父会话FOR UPDATE在原权限下可执行；原POST消息200/409与旧单轮协议保持，真实模型语义仍待。当前教案STREAM_MODEL_CLIENT_MANUAL_STEP.md只新增Python底层生成器，未新增正式HTTP流式接口。流式POST /api/conversations/{id}/messages/stream及delta/result/error/done仅为后续设计；SSE响应已开始后业务409走error事件code而非改HTTP状态，result仅保存成功后发，断连/缺done回读历史而非自动重试。详新教案与MULTITURN_FORMAL_FIXED_ACCEPTANCE.md。

2026-10-19当前验收阻断：会话发送正式版本已接新意图链，但消息最新id的FOR UPDATE缺权限返回500，不能记已接通完成。按MULTITURN_LOCK_FIX_MANUAL_STEP.md仅改消息读取为LIMIT 1 FOR SHARE，父会话FOR UPDATE不变；副本已验证200/409及回滚，正式复验待。HTTP请求仍只content，历史和版本由Java提供，协议无新字段；推荐可选searchIntent/旧单轮兼容已技术检查，真模型语义待。详MULTITURN_ACCEPTANCE.md。以下旧FOR UPDATE消息锁描述为历史教案，已由本条修正覆盖。
## 多轮导购：Python技术验收通过，Java待接（2026-10-19）

2026-10-19最终业务接线教案JAVA_CONVERSATION_WIRING_MANUAL_STEP.md：会话发送拟切换为本人history→/api/conversations/intent；有clarification直接正常保存追问/空卡片，无clarification则用已解析AiIntentData查真实候选并向/api/ask传原question及searchIntent，不再次/api/intent。统一原200/data与原始content保存，锁内版本409保持；原公开单轮ask行为兼容。当前仅用户手写两Service完整替换代码，下一集中本组验收，不能记多轮已接通/真实语义已通过。

2026-10-19推荐请求扩展教案CONVERSATION_RECOMMENDATION_CONTEXT_MANUAL_STEP.md：拟在Python/api/ask原question/products上新增可选searchIntent（keyword/maxPrice），缺省None兼容旧请求。有值时规范化后作为数据供推荐理解当前省略问句，空品类/坏金额不默默推荐；候选仍必须由Java按相同合并条件查库。Java AiClient recommend(request)委托recommend(request,null)保留原请求体，新recommend(request,intent)通过Map带额外字段，旧AiAskRequest不变；无候选/clarification不调用推荐。原问题不改写或截断，当前仅教案未验证该扩展。

2026-10-19保存冲突手写课JAVA_CONVERSATION_CONFLICT_MANUAL_STEP.md：拟新增409 CONVERSATION_CHANGED“会话已有新消息，请刷新后重新发送”。expectedLastMessageId只取Java读取窗口的编号，保存先锁本人会话，再以LIMIT 1 FOR UPDATE当前读本会话最新id（空0），不同则插入前抛异常，不自动重试；不是前端新字段或幂等保证。同步saveTurn第5参数与SendService唯一调用，尚未正式构建/并发验证，多轮业务仍待接。

2026-10-19本人历史窗口教案JAVA_CONVERSATION_CONTEXT_MANUAL_STEP.md：内部ConversationContext(history,lastMessageId)经loadContext先验归属，只查本会话最近10条id/role/content、从旧到新，空列表最新id=0。lastMessageId仅Java保存检查使用，不新增HTTP字段或让前端传版本；当前只有读取教案，没有保存冲突检查或409接线。下一在会话写锁内当前读复核，再多轮推荐编排。

2026-10-19AiClient手写课JAVA_CONVERSATION_CLIENT_MANUAL_STEP.md：parseConversationIntent检查问题/≤10条历史及角色正文后POST新Python接口；内层keyword/规范金额/可空非空白clarification复核，空品类且无追问不合约定，坏上游503、有效追问正常返回。本地格式校验不替代后续会话归属验证。仅代码教案，原parseIntent/公开导购保持，新Java方法未集中运行验证。

2026-10-19最新状态：Python POST /api/conversations/intent正式文件技术验收105通过，200/data三字段、输入422、技术503及原单轮回归成立，见CONVERSATION_CONTEXT_PYTHON_ACCEPTANCE.md。模型为测试替换，真实语义因当前终端无Key上游HTTP0而待验；Java尚未接通。当前给四JavaDTO（JAVA_CONVERSATION_DTO_MANUAL_STEP.md），仅用户手写协议结构，后续仍须严格请求/响应校验、本人历史与保存时409一致性。以下旧“正式循环导入待修”记录已由本次实际导入通过覆盖，完整多轮仍未实现。

2026-10-18当前导入修复/HTTP手写课CONVERSATION_IMPORT_FIX_AND_API_MANUAL_STEP.md：正式intent_schema被误覆盖而循环导入，intent_service缺新函数，尚待用户修复。给main新增POST /api/conversations/intent增量，正常200/data，Pydantic输入422，模型/解析/金额技术错误503 AI_SERVICE_UNAVAILABLE“对话需求提取暂时不可用，请稍后重试”。33临时正确副本预检通过，未修改正式文件，不能当正式接口已上线。下一实际Python集中检查后再Java接线。

2026-10-18Service教案CONVERSATION_CONTEXT_SERVICE_MANUAL_STEP.md：当前question+history JSON作为数据，一次模型调用后ConversationIntent结构校验和共用normalize_intent金额校验，空品类且无澄清补追问。规则：明确新条件优先，预算修改继承用户品类，明确新品类不自动沿用旧预算；模糊相对预算追问数字/不将助手商品价格当用户预算。技术错误继续抛出供后续503，不吞成澄清。以上提示词目标尚未HTTP接线或实测，不当准确率/注入防御保证。

拟新增仅Java调用的Python POST /api/conversations/intent：当前question沿原1000字符规则；history默认[]、最多10条旧→新记录，每条role限user/assistant、content非空白≤2000，不含当前未保存问题。输出沿keyword/maxPrice加clarification（null或非空白≤200）；非null时Java跳过商品查询并保存追问文字。明确最新预算优先，模糊“便宜点”先澄清不猜金额。字段完整规则/业务提示词后课补齐；本课只用户手写conversation_schema.py，不改旧单轮请求schema（推荐/知识共用）。浏览器不上传history/role/userId，Java验归属查本人会话窗口，并记最新消息id，保存锁内复核变化则409不保存陈旧结果；该保护未提供实现，不能将原保存串行视为多轮一致性。参考CONVERSATION_CONTEXT_SCHEMA_MANUAL_STEP.md。原单轮287证据不覆盖此草案。

## 2026-10-18最新已验收：会话内单轮导购发送与快照历史

POST /api/conversations/{conversationId}/messages已在正式79源码JAR验证：USER+Cookie+CSRF，Valid JSON content非空白且最多1000，路径本人会话id，成功200/data为userMessage/assistantMessage/recommendation。消息含id/role/content/createdAt/可空recommendation，助手推荐与顶层同值；user SQL NULL/assistant完整JSON持久化，GET读回，旧消息null。模型在短事务之外，失败不落库；保存故障完整回滚/双用户隔离通过。209本组+78旧AI模拟=287，真实DB而模型模拟，本轮不是新真实DeepSeek→保存实测。未认证POST可能先受CSRF拒绝，不保证所有缺凭据POST必401。历史尚未作为模型上下文。以下草案为历史，以本段/CHAT_MESSAGE_SEND_ACCEPTANCE.md为准。

## 当前新增草案：会话内发送导购问题（2026-10-18，尚未实现）

2026-10-18HTTP入口手写教案CHAT_MESSAGE_SEND_CONTROLLER_MANUAL_STEP.md：POST /api/conversations/{conversationId}/messages拟由已有Controller接Valid MessageSendRequest/Authentication/PathVariable，SendService结果统一data正常200；Security仅精确POST */messages hasRole USER并保留CSRF，原GET同路径保留。当前只提供增量代码，未正式运行验收，不把上轮183当发送验证。下一集中本组构建/DB保存回读/隔离/失败回滚/旧接口回归。

2026-10-18发送编排教案CHAT_MESSAGE_SEND_SERVICE_MANUAL_STEP.md：目标send先验证本人会话/输入，再复用现有单轮AssistantService.ask，实体列表转现有商品摘要/AssistantResponse，再调用独立写Bean保存。Controller入口和精确安全规则待下一课；尚无真实发送/持久化验收，历史不作为模型上下文。本模型技术失败不调用保存，保存阶段重新核验归属；原公开导购接口保持。

2026-10-18保存教案CHAT_MESSAGE_WRITE_SERVICE_MANUAL_STEP.md：独立写服务public事务内重新按当前用户id验证并FOR UPDATE锁会话，序列化结果/成对插入/读回消息DB时间/更新会话DB时间，失败整组回滚。模型始终在后续编排的事务外；本地JSON序列化现安排在短事务内。锁保证该业务入口同一会话保存不穿插，未保证并发模型调用顺序、幂等或多轮上下文。当前尚无发送HTTP接线或正式验收。

2026-10-18当前历史响应扩展教案CHAT_MESSAGE_RECOMMENDATION_JSON_MANUAL_STEP.md：MessageResponse拟保留id/role/content/createdAt并新增可空recommendation（AssistantResponse对象，非JSON字符串）。实体recommendation_json经显式Jackson3转换，旧SQL NULL返回null；已有非空数据转换错误为服务端500，不吞成空卡片。MessageSendResponse既有顶层recommendation暂保留，未来与assistantMessage.recommendation同值。上述代码待用户手写和本组验收，不能据此称正式运行接口已具备卡片恢复功能。

2026-10-18存储准备手写课CHAT_MESSAGE_RECOMMENDATION_STORAGE_MANUAL_STEP.md：拟用chat_messages.recommendation_json JSON NULL保存完整AssistantResponse快照，ChatMessage以String显式映射。旧消息/user消息为空；历史MessageResponse下一课新增可空recommendation对象并由显式JSON转换还原，当前未给代码或验证该响应变更。本轮没有执行011或改正式应用。历史商品信息是当时快照，不作为当前价格/库存承诺。

- POST `/api/conversations/{conversationId}/messages`，ROLE_USER、登录Cookie及CSRF；JSON `{"content":"推荐500元以内的跑鞋"}`，content必填非空白、最多1000字符，与现有模型问题边界一致。会话id在路径、身份取Authentication，userId/role由服务端控制。
- 目标200 `data` 为MessageSendResponse：userMessage和assistantMessage各为现有MessageResponse；recommendation为现有AssistantResponse(keyword/maxPrice/answer/products)。assistantMessage.content与recommendation.answer一致。
- 先验归属，事务外复用AssistantService.ask；结果准备好后用独立短事务复核归属/插入两消息/更新会话时间，提交后返回。模型失败不存本轮；DB保存失败整对回滚。沿用400/401/403/404/503，意外错误500。当前无幂等保证，网络丢失响应后重发可能重复，禁止默认自动重试；多轮前补会话并发规则。
- 当前chat_messages只有文字；必须先扩展可空JSON结果字段与历史读取响应，旧消息为null，保存推荐快照，避免刷新丢商品卡片。字段迁移/映射/读取变化尚未给出或执行。
- 本课只提供MessageSendRequest/MessageSendResponse手写代码，尚无发送Controller/安全放行/持久化实现；详见CHAT_MESSAGE_SEND_DTO_MANUAL_STEP.md。原已验收历史读取接口保持。

2026-10-18最新已验证状态：Java知识接口已实现；用户补齐Textual严格转换后正式49源码构建与78项模拟/安全/旧AI回归全部通过，原3项失败已修复。真实3样例及原文快照来自同日上轮正式链，本轮不重复收费调用，详见JAVA_KNOWLEDGE_ACCEPTANCE.md。以下旧“待修复”描述保留过程，以本段/最新正式证据为准。

阶段6最新：注册历史118项保持；2026-10-18用户手写登录/me/logout正式60源码构建、69会话HTTP+6Provider模拟Mapper+27注册输入+78AI模拟回归共180项通过。后端三接口已开放验证，详见AUTH_SESSION_ACCEPTANCE.md；网页账号接线/私有历史/多轮/SSE仍待实现。旧草案/手写记录为历史，以本段和最新正式证据为准。

## 阶段6登录、当前用户、退出（2026-10-18最小后端已验收）

### 私有会话/历史：创建、本人详情、列表与消息读取已验收

#### 当前正式列表/消息读取（2026-10-18）

- GET /api/conversations默认page1/size10，size1..50/page>=1，200 data含items/page/size/total/totalPages；总数和记录均本人user_id，更新时间/id双倒序。空参数按默认值，非法格式/溢出/范围400，超末页200空items；无会话账号空统计由实际服务合成练习身份验证，普通A/B HTTP隔离已实际验证。
- GET /api/conversations/{conversationId}/messages，ROLE_USER/Cookie，size默认20/1..50，beforeId可选正Long排他上界，省略/空按null；返回最近批、id升序、hasMore及更早游标，最后一批/空批为false/null。归属优先，非本人/不存在404，本人空200；格式/范围400，匿名401、ADMIN无USER403、DB故障503。有效其他会话或不存在的游标编号仍只作为上界，不改变父会话。
- 正式74源码/105本组检查与78AI模拟回归通过，见CHAT_HISTORY_READ_ACCEPTANCE.md。首次中文比对失败是探针编码错误，已修正辅助输出且只读复用原样例重跑，应用源码未改。消息为7条明确标记验收样例；正式消息POST/问答自动入库/多轮/SSE尚未实现，未开放任意role写入。下方目标教案保留历史，不覆盖本节实际结果。

#### 消息历史准备课契约（历史，已由上方验收更新）

- 当前HTTP课CHAT_MESSAGE_HISTORY_CONTROLLER_MANUAL_STEP.md：用户修改已有Controller，替换两Service构造器（只保留一个）、新增GET /{conversationId}/messages并在Security会话GET规则加精确消息模式仍USER，旧三个方法不重复。可选beforeId省略/空参数按null，size省略/空参数默认20；非空非法格式400。未构建运行，下一列表+消息一组正式验收，不能把手写接线当端点已验证。

- 当前查询课CHAT_MESSAGE_HISTORY_SERVICE_MANUAL_STEP.md：ChatMessageService.readHistory先调用getOwned检查当前用户会话，构造conversation_id+可选id<beforeId，Page(1,size+1,false)只取记录无COUNT，多取判断/去额外/升序显示/组游标；尚无Controller/Security新路由或运行证明，下一接线后与列表一起验收。空本人会话正常false/null，不把内存截断当数据库删除。

- GET /api/conversations/{conversationId}/messages，ROLE_USER/Cookie，GET无CSRF头。先验证会话归属，不存在与非本人同404 CONVERSATION_NOT_FOUND；本人无消息200空列表。conversationId正Long，size int默认20范围1..50，beforeId可选正Long排他上界；非法格式/溢出/范围400，匿名401、无USER403、账号不可用401、DB503。
- 未传beforeId取最近size条；传入只取本会话id<beforeId。数据库id降序取size+1，多取1条判断hasMore，截断后按id升序返回。200 data=MessagePageResponse(items,nextBeforeId,hasMore)，items为MessageResponse(id,role,content,createdAt)；有更早消息时nextBeforeId=本批最小id，否则null，空items时false/null。无需cursor本身存在，但它不能改变会话归属。
- 当前CHAT_MESSAGE_HISTORY_DTO_MANUAL_STEP.md只给两DTO/协议，无查询服务或HTTP接线，未编译运行，旧275不涵盖本功能。下一消息服务和精确路由后与列表集中验收，不把读历史等同已保存消息/多轮模型上下文。会话列表继续page/size契约不变。

#### 本人会话列表准备课契约（历史，已由上方验收更新）

- GET /api/conversations?page=1&size=10，ROLE_USER/Cookie，GET无CSRF头要求。page为int默认1且>=1，size为int默认10且1..50；非法格式/越界/范围400，无认证401、无USER403、普通账号不可用401、DB故障503。
- 200 data=ConversationPageResponse(items,page,size,total,totalPages)，items为ConversationResponse四字段数组；条目和total都只计当前user_id，不接受前端userId覆盖。按updated_at DESC,id DESC，空历史items=[]/total0/totalPages0，超末页items=[]保留请求页码，不404。
- 当前CHAT_CONVERSATION_LIST_MANUAL_STEP.md给新分页DTO和Service/Controller/Security完整增量；复用现有分页拦截器，未构建运行，不能将旧275创建/详情验收当列表已通过。消息读取入口仍未开放；下一与消息读取一组验收。页码分页不承诺跨请求动态更新快照一致性。

#### 当前正式已验收接口（2026-10-18）

- POST /api/conversations：ROLE_USER、JSON title非空白/原始≤100、同会话有效CSRF；服务端决定userId，strip标题，返回201、Location /api/conversations/{id}、data四字段id/title/createdAt/updatedAt。默认时间与实际数据库匹配，额外userId/id等不能覆盖服务端值。
- GET /api/conversations/{id}：ROLE_USER；本人200、data同四字段，不存在或非本人统一404 CONVERSATION_NOT_FOUND“会话不存在”；URL非法编号400，匿名401，独立ADMIN无USER角色403。query userId不改变归属；GET无CSRF头要求。
- 缺/错/跨会话POST CSRF403，非法JSON/标题400，已登录用户遇数据库断连业务503 SERVICE_UNAVAILABLE。退出/重登仍可读取持久化会话。会话列表和消息子路径当前尚未开放，消息未保存。
- 正式70源码构建，101会话检查（含实际结构授权及8模拟Mapper边界）与69/27/78旧功能回归通过，详见CHAT_CONVERSATION_ACCEPTANCE.md。本轮无真实模型调用，禁用账户分支仅模拟未修改真实账号。下方准备课中的未实现/未开放为历史，以本段和证据为准。

#### 创建会话准备课历史契约（2026-10-18，已由上方验收更新）

- 当前接线课CHAT_CONVERSATION_CONTROLLER_MANUAL_STEP.md：用户新建Controller POST创建返回201/Location/data，GET /{id}返回200/data；Security精确POST基础地址/GET一层id要求ROLE_USER，保留CSRF和原admin/公开规则。尚未构建运行，不宣称已开放；下一轮集中正式验收后更新状态。GET列表/消息子路径未开放，独立ADMIN不是USER。

- 配套本人详情GET /api/conversations/{id}目标：ROLE_USER，200 data复用ConversationResponse四字段；GET不要求CSRF头，身份仍由Cookie关联。编号非正/格式非法400、无身份401、无USER权限403、数据库故障503；不存在或非本人统一404 CONVERSATION_NOT_FOUND“会话不存在”。当前CHAT_CONVERSATION_READ_SERVICE_MANUAL_STEP.md只给getOwned/自定义异常/全局404教案，SQL按id和当前user_id同时过滤，无Controller或正式运行证明；下一精确安全规则接线。

- 当前Service课CHAT_CONVERSATION_CREATE_SERVICE_MANUAL_STEP.md：Transactional create(Authentication,ConversationCreateRequest)，服务端解析userId，原始title非空白且≤100再strip，INSERT后检查1行/生成id，按id+user_id读回完整行与DB默认时间，构造ConversationResponse。失败运行异常回滚；尚无HTTP入口/角色接线或正式运行证据，下一本人读取404和Controller/Security。

- POST /api/conversations，application/json，ROLE_USER+同会话CSRF，JSON {"title":"选跑鞋"}；title必填NotBlank、原始Java长度≤100，Service将strip后存。只用title，userId由当前认证服务端查得，不允许前端指定归属。
- 成功201，Location /api/conversations/{新会话id}，响应data对应ConversationResponse(id,title,createdAt,updatedAt)。时间由后续Service读回数据库默认时间，ISO本地时间不伪装UTC。GET /api/conversations/{id}本人详情为后续目标，非已开放接口。
- 目标400非法输入/JSON类型，401未登录或账号不可用，403无USER权限/CSRF失败，503数据库故障；前置安全可能先于输入校验。CurrentUserService异常在ControllerAdvice中401，数据库503保持。
- 当前手写课CHAT_CONVERSATION_DTO_MANUAL_STEP.md仅ConversationCreateRequest/ConversationResponse代码与协议，不代写/运行，创建接口尚未开放。列表和消息分页完整协议在对应课补齐。

#### 本模块准备课与归属规则

- 当前用户id课CHAT_CURRENT_USER_MANUAL_STEP.md：后续Controller显式把当前Authentication传CurrentUserService.requireUserId，服务端查询username对应id/status，拒绝匿名/无账号/非正常账号，经新增ControllerAdvice AuthenticationException处理返回401 UNAUTHORIZED“登录状态无效，请重新登录”；DB异常仍原503。登录过滤器回调不变，此服务本身不开放新地址或清Session，现有me仍仅认证用户名。不据教案宣称私有接口已接线。

- 当前映射课CHAT_MESSAGE_MAPPERS_MANUAL_STEP.md：用户手写ChatMessage和两个BaseMapper；仅持久化组件教案，未新增HTTP地址/响应或信任前端role/userId。下一先服务器解析当前app_users.id，再补完整私有接口契约，仍待集中验证。

- 授权课CHAT_HISTORY_GRANTS_MANUAL_STEP.md：Java的既有MySQL账号shopagent_app@127.0.0.1，仅新增chat_conversations SELECT/INSERT/UPDATE和chat_messages SELECT/INSERT。用户用原root执行010；应用账号不同于app_users中的普通用户，SQL授权不代替服务端user_id归属检查。未执行/验收权限，尚无私有HTTP接口。

- 建表课CHAT_HISTORY_TABLES_MANUAL_STEP.md：chat_conversations.user_id关联app_users.id、chat_messages.conversation_id关联会话，消息只存user/assistant文字最小结构，卡片/引用后续扩展。登录Session失效不删除历史。
- 归属从Authentication用户名由服务端查询普通用户id，前端userId不可信；新普通聊天路由要求ROLE_USER，不自动让独立配置管理员读取普通用户历史。列表按当前user_id筛选，按id读会话同时校验user_id，读消息也先验证会话归属；不属于自己与不存在都目标404，未认证401。
- 拟定POST/GET /api/conversations、GET /api/conversations/{id}/messages；当前无端点/DTO/分页实现，完整HTTP契约在对应代码前补齐。角色由后端控制，不开放客户端任意写assistant的通用消息入口；仍保留既有CSRF策略。
- 本课只有两表DDL教案，助手只读元数据、未代建正式009或执行DDL/授权，用户操作待后续集中验收，不把表存在或FK当数据权限已实现。以下登录段落仍以最新正式验收为准。

### 已验收登录接口与历史课记录

- 当前生效：POST登录表单+有效CSRF→200 authenticated true；GET me需认证→200 data.username，无身份401；POST退出需有效CSRF→204无体、清会话与JSESSIONID。成功登录/退出后刷新CSRF；旧Cookie/令牌失效，重复退出先刷新有效令牌。普通USER不能访问ADMIN。以下各课“待实现/未开放”描述保留历史，不覆盖此正式结果。

- 当前退出课AUTH_LOGOUT_MANUAL_STEP.md：Security内置LogoutFilter处理POST /api/auth/logout，clearAuthentication/invalidateHttpSession/deleteCookies JSESSIONID，成功回调204无响应体；保留同会话CSRF，无需新增Controller或公开URL规则。登录成功后先重新GET csrf再退出；退出后再次POST也重新取csrf，旧令牌403。重复退出以携带当前有效CSRF为前提。GET不执行退出，可能出现框架确认页。清理Session不撤销Basic凭据，Cookie验收不附Authorization。本课未写正式应用或运行，下一与me/登录集中验收。

- 当前课AUTH_CURRENT_USER_MANUAL_STEP.md：AuthController新增GET /me(Authentication)，成功仅data.username；Security精确GET /api/auth/me authenticated置于denyAll前，不加入公开GET列表。身份来自当前认证而非请求字段；本实现name为用户名，非参考项目的用户id。普通USER/ADMIN均可，无有效身份401；不直接返回Authentication或哈希。仅教案，用户手写后与POST退出集中正式验收，尚未构建或运行。

- POST /api/auth/login采用application/x-www-form-urlencoded表单username/password，与注册JSON不同；将由框架formLogin处理。匿名可提交，保留同会话CSRF。成功200 `{"data":{"authenticated":true}}`，框架服务端Session保存身份，浏览器Cookie关联；本响应不含JWT或CSRF令牌。
- GET /api/auth/me需已认证，200 `{"data":{"username":"xiaowang"}}`，username从当前认证获取，不信任请求指定用户名/userId。未认证401；不直接返回UserDetails/密码哈希。后续私有数据在服务器确定用户ID。
- POST /api/auth/logout保留CSRF，目标204无响应体，清理认证/失效Session/Cookie；可重复退出。登录成功和退出后旧CSRF清除，后续POST前重新GET /api/csrf。GET登录/退出不作为业务操作。
- 错误/缺失/非法凭据或禁用账号统一401 INVALID_CREDENTIALS；认证服务故障503 AUTH_SERVICE_UNAVAILABLE（安全提示登录服务暂时不可用）；前置CSRF拒绝403 FORBIDDEN。下一登录过滤器接线要在密码检查前限制输入，尤其UTF-8密码≤72字节，注册DTO不会自动校验表单。
- 方案选择与回调定义不代表已开放接口；本课没有修改正式Java/安全过滤器、安装Redis或运行会话测试。Basic现有调试能力保留，会话验收明确不附Authorization，防止把Basic身份误认成Cookie持久化。
- 当前登录配置教案AUTH_FORM_LOGIN_MANUAL_STEP.md：表单username完整匹配注册用户名格式，password非空白/Java长度≤64/UTF8≤72，在密码验证前拒绝越界为401；短错误密码也统一401，不另设登录最小长度。Session IF_REQUIRED并在登录成功时更换会话编号，Handler配置明确绑定，CSRF保留；仍待用户手写后的正式验收。
- 上述候选副本JAR35项HTTP/会话/角色/CSRF与认证故障检查通过，仅读已登记练习数据，无正式源码/JAR改动；证据verification/login-form-preflight-2026-10-18.json，不把候选结果当正式登录、me或退出完成。

## 阶段6注册接口约定（2026-10-18草案，未开放）

- POST /api/auth/register，application/json；RegisterRequest含username/password。username完整匹配`[a-z][a-z0-9_]{2,31}`，密码NotBlank且Java长度8～64；后续Service复核UTF-8编码字节边界（当前默认BCrypt最多72字节），不得trim密码。保留用户名shopagent_admin后续Service拒绝；普通注册不得自选管理员角色。
- 成功目标HTTP201，`{"data":{"id":1,"username":"xiaowang"}}`，id仅示例，由MySQL生成；内层UserResponse只含id/username，不返回密码/哈希/认证令牌，注册后不自动登录。
- 目标错误：参数/保留名400 INVALID_ARGUMENT；用户名重复含并发唯一冲突409 USERNAME_ALREADY_EXISTS；CSRF缺失/错误403 FORBIDDEN；数据库不可用/权限问题503 SERVICE_UNAVAILABLE。重复冲突新异常处理、通用数据库错误提示待后续课实现，不把当前500当已覆盖。
- 未来匿名放行精确注册POST但保留同会话CSRF；本轮不改Security、Controller、数据库权限或日志。现有应用数据库账号权限尚未扩到app_users。完整教学/字段来源见AUTH_REGISTER_DTO_MANUAL_STEP.md，表/DTO/实际接口留集中验收。
- Service本课候选见AUTH_REGISTER_SERVICE_MANUAL_STEP.md：使用默认bcrypt编码器，UTF-8密码最多72字节，保留管理员用户名400；同名预查和数据库DuplicateKey冲突都抛UsernameAlreadyExistsException，由新增具体处理映射409。Transactional保证正常提交/运行时异常回滚，返回UserResponse仍不自动登录。正式代码尚待用户手写与集中验证；当前商品专用数据库错误文案在下一课通用化。
- Controller课AUTH_REGISTER_CONTROLLER_MANUAL_STEP.md给完整入口和安全增量；目标数据库503 message改为“数据库服务暂时不可用，请稍后再试”，原HTTP状态/错误码保持。Valid只有经过前置安全检查后才执行，不保证缺CSRF的非法输入返回400。正式运行尚待验收，不能把教案视为接口已开放。

## 阶段5知识问答约定（2026-10-18 Java真实链已通，严格类型待修复）

最新状态覆盖下方较早的待实现描述：Java POST /api/assistant/knowledge与匿名POST精确放行/保留CSRF已经实现。49源码构建成功、3次真实回答及来源快照通过；60模拟/安全检查57通过，3项上游数字→String应拒绝但未拒绝。全部文本字段应严格字符串；候选Textual配置隔离78项通过，正式代码仍等用户手写补充，不记为已修复。当前入口JAVA_KNOWLEDGE_TEXTUAL_FIX_MANUAL_STEP.md，证据JAVA_KNOWLEDGE_ACCEPTANCE.md。Python历史70项仍按原范围；后续文字保留设计与实现过程，不能据较旧描述推断路由不存在。

2026-10-18Java当前手写入口JAVA_KNOWLEDGE_CONTROLLER_MANUAL_STEP.md：独立KnowledgeController的POST /api/assistant/knowledge，构造器注入AiClient、Valid/RequestBody复用IntentRequest；返回Map data。SecurityConfig仅在原精确POST放行列表增加此地址，匿名问答但保留CSRF，管理员/其他拒绝策略保持。代码完成状态/编译/新端点与权限边界仍待下一集中验收，不能仅凭教案认为已可调用。

Python正式知识路由与本轮模块已于2026-10-17集中验收：59模拟+11运行断言（含3次真实DeepSeek请求）共70项通过，见RAG_PYTHON_ACCEPTANCE.md与verification/rag-knowledge-2026-10-17.json。复用IntentRequest；非法请求422，正常无依据200，已知模型HTTP/文件OSError/结构引用错误503 AI_SERVICE_UNAVAILABLE，意外错误500，成功data包结果。Java知识DTO当前手写课，Java入口/安全配置未改。

下列Python行为已按上述范围验证，Java部分仍为待实现目标。数据格式见RAG_SCHEMA_MANUAL_STEP.md，独立知识问答未混入既有商品推荐入口。

- Python内部已实现`POST /api/knowledge/ask`、Java对外待实现`POST /api/assistant/knowledge`。请求JSON为`{"question":"K1的充电接口是什么？"}`，question沿现有非空白、1～1000字符限制；Python非法请求默认422，Java计划400。Java新增路由后续接入现有Security/CSRF规则，不因写草案放宽权限。
- 模型草稿KnowledgeDraft：answer为1～2000非空白字符串，answerable为严格布尔值，citedIds为最多3个片段编号。Python Service已校验编号属于本次候选（空/空白不匹配），并按序去重；true至少一个引用，false必须空引用。这些跨字段/候选关系由Service完成，Field负责结构。
- HTTP成功200为`data`包裹KnowledgeResult：answer、answerable、sources。每条KnowledgeSource含chunkId、source（文件名）、document（标题/演示声明）、section、content，全部由Python从检索候选快照组装。Java后续复核基本格式和true/false引用一致性，不用模型捏造文件名/原文。
- 资料不足是正常200：`{"data":{"answer":"现有资料不足以回答这个问题。","answerable":false,"sources":[]}}`。非法模型JSON、截断、候选外引用、已知HTTP/文件系统错误返回503统一错误，不能用无资料回答掩盖故障；未预期错误保留500。配置仅服务端既有环境读取，未将Key写进输入/输出或读取真实值。
- 相似度仅用于检索排序，不直接当回答概率。合法引用不能证明每个断言有依据；USB-C/包装无充电头/未知防水等样例和故障回归需实际验收后才称实现。
- 本节不返回新的商品价格/库存事实，不修改MySQL商品、推荐接口、用户权限或数据库。不进行网络搜索，先限于演示知识资料。

版本：0.1；日期：2026-10-11（Asia/Hong_Kong）。状态：健康接口已实现并独立检查 HTTP 200/JSON 字段；停止服务后的检查仍待做。商品列表/详情已实现，2026-10-12正式200/400/404/503与字段验收通过，证据verification/product-list-2026-10-12.txt；下列原始示例用于字段约定。用户已决定先补商品管理CRUD；Python健康接口保留草案，稍后再实施。

## 1. 这份文档有什么用

前端发什么、Java 返回什么，需要先说清楚。后续 Java 调 Python 时再增加内部服务约定。每完成一个接口，把状态改成“已实现/已验证”，记录实际检查证据。

原项目的 `docs/设计文档.md` 第 9 章是接口清单：适合了解功能，但缺少本阶段所需的完整参数、字段和失败示例。我们逐功能维护自己的约定。

## 2. 第一阶段范围与通用规则

Java 本机基址：`http://127.0.0.1:8888`。启动类与健康接口已运行检查；商品列表与详情已实现，正常/参数/缺失/隔离数据库故障响应已验证。

| 接口 | 用途 | 开发顺序 | 当前状态 |
| --- | --- | --- | --- |
| `GET /api/health` | 看 Java HTTP 服务是否能响应 | 第一课 | 已实现；HTTP 200、application/json、两个字段和值已核实；停止后检查待做 |
| `GET /api/products` | 分页查看/搜索商品 | 商品课 | 已实现/已验证：14项成功与9项非法参数、DTO字段与数据库故障503通过 |
| `GET /api/products/{id}` | 查看一个商品 | 商品课 | 已实现/已验证：200/400/404和正式数据库故障503通过，未预期500此前候选隔离验证 |

这是我们自己的推荐地址。原项目的列表是 `/api/product/list`，不是 `/api/products`；自己的页面按本文件开发，不混用两套地址。开始对应模块时可调整草案并同步全部调用方。

- 这三个接口计划允许匿名读取；第一阶段没有修改业务数据的接口。
- JSON 字段用 camelCase；响应类型为 `application/json`。
- 成功使用 HTTP 200 与 `data` 字段；失败使用相应 HTTP 状态码与 `error` 字段。HTTP 状态码是协议的一部分，不把所有错误都包装成 200。
- 商品 ID 为正整数；本阶段样例数据在 JavaScript 安全整数范围内。价格单位为人民币元，以两位小数的字符串返回，例如 `"299.00"`；Java 内部使用精确十进制类型。页面展示字符串，业务计算由后端完成。
- 错误不返回数据库密码、SQL 原文或内部堆栈。

## 3. 健康接口：第一课

用途：检查服务能否处理 HTTP 请求。这里的 `UP` 只表示本接口能响应，不表示数据库、Python 或模型服务都正常。

请求：`GET http://127.0.0.1:8888/api/health`。无请求参数、无请求体、无需登录。

成功：HTTP 200。

```json
{
  "data": {
    "status": "UP",
    "service": "shopagent-backend"
  }
}
```

| 字段 | 类型 | 含义 |
| --- | --- | --- |
| `data.status` | string | 固定为 `UP` |
| `data.service` | string | 服务名称，固定为 `shopagent-backend` |

验收：服务启动后用浏览器访问此地址，收到上述结构；停止服务后连接失败。服务未启动时无法返回 JSON，不期待此时仍有某个业务错误码。

## 4. 商品列表与搜索

请求：`GET /api/products?page=1&size=10&keyword=跑鞋`。无请求体。

| 查询参数 | 类型 | 必填 | 默认值 | 规则 |
| --- | --- | --- | --- | --- |
| `page` | integer | 否 | 1 | 大于等于 1 |
| `size` | integer | 否 | 10 | 1～50 |
| `keyword` | string | 否 | 不过滤 | 去掉首尾空白后最多 50 字符；空字符串视为不过滤 |

初版仅返回上架商品；关键词匹配标题或品牌，普通关键词做SQL LIKE包含查询；%与_按SQL LIKE通配符处理。暂不加入同义词扩展或向量检索。固定按 ID 升序，避免分页排序不稳定。

成功示例：HTTP 200。

```json
{
  "data": {
    "items": [
      {
        "id": 1,
        "title": "示例轻量跑鞋",
        "brand": "示例品牌",
        "price": "299.00",
        "imageUrl": null
      }
    ],
    "page": 1,
    "size": 10,
    "total": 1,
    "totalPages": 1
  }
}
```

- `items` 为数组；商品 `id` 为 integer，`title`/`brand`/`price` 为 string，`imageUrl` 为 string 或 null。
- `page`、`size`、`total`、`totalPages` 为 integer；`total` 是过滤条件下的商品总数，不是当前页数量。
- 无匹配时，HTTP 200、`items: []`、`total: 0`、`totalPages: 0`。
- 超出最后一页时返回空数组，保留实际 `total`/`totalPages`，不报商品不存在。
- `page=0`、`size=100`、非整数分页参数或过长关键词：HTTP 400，错误码 `INVALID_ARGUMENT`。

验收：与种子数据比对总数和分页；标题/品牌关键词匹配正确；无结果为数组；下架商品不出现；非法参数返回约定错误。

## 5. 商品详情

请求：`GET /api/products/1`。路径参数 `id` 必须为正整数，无请求体。

成功示例：HTTP 200。

```json
{
  "data": {
    "id": 1,
    "title": "示例轻量跑鞋",
    "brand": "示例品牌",
    "price": "299.00",
    "imageUrl": null,
    "description": "用于验证接口的示例商品介绍。"
  }
}
```

字段与列表单个商品一致，增加 `description`（string）。初版不包含 SKU、评价、库存与订单信息，商品课再讨论扩展。

- 不存在或下架的商品：HTTP 404，`PRODUCT_NOT_FOUND`。
- 非整数、超出可接受整数范围或小于 1 的 ID：HTTP 400，`INVALID_ARGUMENT`。
- 数据库连接/访问不可用：HTTP 503，`SERVICE_UNAVAILABLE`。
- 未预期的内部错误：HTTP 500，`INTERNAL_ERROR`。

验收：已知 ID 与数据库字段一致；无效 ID、不存在 ID、下架 ID 和数据库不可用分别检查。

## 6. 错误格式

例如：HTTP 404。

```json
{
  "error": {
    "code": "PRODUCT_NOT_FOUND",
    "message": "商品不存在或已下架"
  }
}
```

| HTTP 状态 | `error.code` | 含义 |
| --- | --- | --- |
| 400 | `INVALID_ARGUMENT` | 请求参数格式或范围不正确 |
| 404 | `PRODUCT_NOT_FOUND` | 商品不存在或已下架 |
| 503 | `SERVICE_UNAVAILABLE` | 商品查询所需的数据库不可用 |
| 500 | `INTERNAL_ERROR` | 未预期错误，返回简短说明 |

`error.code` 与 `error.message` 均为 string。列表与详情共用以上格式；健康接口不连接数据库。

## 7. 后续接口按模块再设计

以下是设计待办，没有最终地址或字段承诺：

| 模块 | 开发前必须说清楚 |
| --- | --- |
| Java → Python | 消息字段、候选商品/查询边界、回答与卡片结构、超时和模型失败如何映射 |
| AI 对话 | 普通接口的请求/响应、商品 ID/价格的来源、无匹配与模型失败的表现 |
| 登录与会话 | Token 获取/使用、服务器识别用户、私有会话归属、历史分页 |
| SSE 流式 | 事件名称/顺序/载荷、结束与失败、前端解析、与普通接口一致性；POST 流式读取方式 |
| 购物车工具 | 先有 Java 购物车协议，再定确认、取消、重复请求、权限、工具失败和结果 |

参考事实：Java 对话入口是 `/api/chat/messages` 与 `/api/chat/stream/messages`；Python 实际路由是 `/api/ask` 与 `/api/ask/stream`。参考设计文档写的 Python `/api/chat` 不应直接作为联调地址。

## 8. Markdown 与自动生成文档怎样配合

编码前用本文件讨论行为；编码后补充 DTO/校验/注释，并按实际实现生成 OpenAPI / Swagger 页面，再比对是否与文档一致。接口工具可用于联调，第一课不要求先安装 Apifox。

参考 Java 已引入 springdoc 依赖；具体界面能否访问还要实际启动确认。FastAPI 默认提供 `/docs` 与 `/openapi.json`，运行对应服务后再验证。不要把文档工具的默认地址当作当前本机已经存在的服务。

- [springdoc 官方说明](https://springdoc.org/)
- [FastAPI 官方：自动接口文档](https://fastapi.tiangolo.com/tutorial/first-steps/)

## 9. 状态记录

| 日期 | 变化 | 实际验证 |
| --- | --- | --- |
| 2026-10-11 | 创建第一阶段草案；核对参考接口清单与源码 | 文档检查；无应用实现或运行验收 |

## 10. Python服务第一步：健康接口草案

日期2026-10-12；状态：专用环境已准备，用户应用代码待创建，尚未启动验证。
GET http://127.0.0.1:8000/health，无参数、请求体、模型或数据库调用。
成功HTTP200、application/json：{"data":{"status":"UP","service":"shopagent-ai"}}。
UP只表示HTTP接口能响应，不证明模型可用。绑定127.0.0.1，下一步Java调用此接口；超时与未启动行为在跨服务课设计验收。
完整教学：PYTHON_SERVICE_STEP.md。后续模型请求协议另行补齐。
## 11. 商品管理CRUD扩展草案（2026-10-12）

2026-10-13实现更新：下表六个商品管理路由已由用户实现并通过当前正式JAR/真实MySQL的基础HTTP/SQL验收，成功状态、图片清空、逻辑删除与公开/管理视图符合本节协议。006已授本表SELECT/INSERT/UPDATE，新练习行6已逻辑删除、原五条不变。实际依据见PRODUCT_CRUD_ACCEPTANCE.md；测试进程仅使用临时管理员凭据覆盖，真实管理员凭据登录/理解仍待回补。以下2026-10-12“待写权限/待验收”叙述保留为历史，不作为当前状态。

用户明确先CRUD再Python。此节是阶段2b协议：管理员配置课程/候选验证已准备，逻辑删除数据库迁移已执行，用户新增Service已写；当前新增管理响应DTO/POST Controller课程见PRODUCT_CREATE_CONTROLLER_STEP.md，正式写HTTP/鉴权与校验仍待集中验收，尚未授数据库写权限。公开GET仍按前述协议只返回上架且未删除商品；管理路由需管理员访问控制，能查看下架但不显示已删除商品。

| 方法与地址 | 功能 | 预期成功 |
| --- | --- | --- |
| GET /api/admin/products | 分页搜索所有未删除商品，含下架 | 200、含status的管理列表DTO |
| GET /api/admin/products/{id} | 管理详情，含下架 | 200、含status的管理详情DTO |
| POST /api/admin/products | 新增商品 | 201、Location管理详情地址、data管理详情 |
| PUT /api/admin/products/{id} | 完整替换可编辑商品字段 | 200、data管理详情 |
| PATCH /api/admin/products/{id}/status | 设置上架1/下架0 | 200、data管理详情 |
| DELETE /api/admin/products/{id} | 逻辑删除（计划deleted1） | 204、无响应体 |

C/PUT请求白名单字段：title必填非空白最长200；brand必填非空白最长100；price必填BigDecimal、非负、最多8位整数和2位小数（例如JSON字符串329.00）；imageUrl允许null/空白（转null），最长500，非空需HTTP(S)地址；description必填可为空字符串，最长10000；status必填0/1。不接收用户指定id/deleted。

PATCH status请求：{"status":1}。PUT语义包含所有可编辑字段，imageUrl为空可清空原图片，实现时不能被MyBatis默认忽略null策略跳过；业务字段写入前做规范化，未知字段/精度/坏JSON与校验失败策略在Controller课实际验证。

预期错误继续error.code/message：400 INVALID_ARGUMENT，401 UNAUTHORIZED，403 FORBIDDEN，404 PRODUCT_NOT_FOUND，503 SERVICE_UNAVAILABLE，500 INTERNAL_ERROR。401/403由访问控制层处理，不能假定ControllerAdvice能捕获Security过滤器异常。POST成功返回服务器生成的编号；已删/缺失对象更新或详情404，重复DELETE拟404，最终实现前核对。

逻辑删除采用deleted字段与TableLogic，删除标记不向公众暴露；status0是仍可管理的下架商品，deleted1是从业务视图删除。2026-10-12已执行005迁移并验证原五条记录全部未删除，Java映射/写接口效果仍待集中验收；完整用户注册/JWT留后续阶段6，管理起步凭据在外部本机配置，不能使用数据库root密码或写进前端。

本课起步与完整DTO：PRODUCT_CREATE_REQUEST_STEP.md。先完成管理员边界与接口代码，再授予专用账号products表SELECT/INSERT/UPDATE（逻辑删除无需DELETE），不变更密码或其他库。验收仅新建练习数据，原五条不用于删除/重置。

## 12. 阶段2b/3手机课程的管理调试协议候选

2026-10-15最新状态：回退后用户已手写重做POST /api/admin/ai/echo及三个Java DTO；当前29源码编译与24项接口验收全部通过，新证据verification/ai-echo-manual-2026-10-15.json。请求/响应约定仍见本节；真实Python正常往返与模拟上游故障分别验证。下面回退记录及更早报告仅历史。真实模型仍未实现。

2026-10-15回退后当前状态：用户恢复到Java回声DTO步骤开始前，GET /api/admin/ai/health保留且回归8项通过；POST /api/admin/ai/echo及Java Echo DTO已撤销，尚未实现。Python /api/echo为10:40已有代码，保留。此节下方较早“POST已实现/24项通过”仅回退前历史。见ROLLBACK_2026-10-15_1043.md。

2026-10-15：POST /api/admin/ai/echo现已正式实现并通过当前JAR/用户Python真实POST回声往返、400/401/403/503及模拟上游异常/超时共24项验证，见verification/ai-echo-2026-10-15.json。请求message非空白且最多1000字符；200 data.message为原文、service为shopagent-ai。GET健康与POST回声都已技术验收，此节下文“候选/未实现”为较早状态，真实模型仍未接。

2026-10-14：GET /api/admin/ai/health正式实现并通过当前Java JAR→用户Python的HTTP200/停依赖503验收，Java自身健康保持200，见verification/ai-health-2026-10-14.json。POST /api/admin/ai/echo仍下一课待实现；本節下方更早“健康路由仅候选”记录已被此更新覆盖。真实模型未接。

2026-10-13：GET /api/admin/csrf正式实现及令牌/cookie与修改请求校验已在本机技术验收中通过；用户理解/真实凭据登录暂缓。下面两个管理AI健康/回声路由仍是阶段3教材候选，正式应用尚未实现。

日期2026-10-12。教材候选已隔离验证，正式应用未实现/验收这些新增路由；不能把候选当正式事实。

- GET /api/admin/csrf：管理员Basic，200 data.token/headerName，保留对应JSESSIONID cookie；管理POST/PUT/PATCH/DELETE带同cookie与该头，CSRF保持开启。
- GET /api/admin/ai/health：管理员，Java真实调用Python GET /health，200 data.status/service；依赖失败503 AI_SERVICE_UNAVAILABLE。
- POST /api/admin/ai/echo：管理员+CSRF，JSON {"message":"..."}，Java转发Python /api/echo，200 data.message/service。Javamessage非空白最长1000，失败400；回声是通信验证，不是模型能力。

阶段3先健康/回声，再选定模型与真实调用验证；真实模型未接前不把整个阶段3标已验收。DTO、超时、HTTP/1.1兼容设置与故障处理见手机第二册。

## 13. 阶段3模型衔接与阶段4导购候选

2026-10-17首版Web接入：匿名GET /shopagent/**提供HTML/CSS/JS/SVG（用户加规则并已实测），网页只调Java：GET /api/csrf+同会话Cookie/令牌POST /api/assistant/ask、GET /api/products（items/page/size/total/totalPages）、GET /api/products/{id}。前端fetchData已提取外层data，模型文字按textContent渲染，商品卡片取Java字段，预算null明确显示未设上限。本次真实页面跑鞋500成功、27 HTTP/隔离日志断言及浏览器场景完成，见WEB_FIRST_VERSION_ACCEPTANCE.md；阶段5以后及React/Vite仍非已实现。

2026-10-17严格读取修正闭环：用户配置写入后正式45源码构建、24项定向回归全通过，数字字符串/小数编号严格503，合法推荐/旧AI客户端正常，见verification/java-strict-regression-2026-10-17.json。原Java推荐验收小数失败关闭；本轮不重复收费模型调用，真实200仍为修正前样例。下一用户手动补无依据比较提示规则（RECOMMENDATION_FACT_WORDING_MANUAL_STEP.md），再Web与文字评审，JSON编号保证不等于全部自然语言事实保证。

2026-10-17Java完整链检查：45源码正式构建，真实完整推荐200，59断言通过/1失败；JSON小数编号1.5被默认转换成Long1误成功，推荐编号原始JSON必须为整数的边界尚不满足。当前整组验收未通过，见JAVA_RECOMMENDATION_ACCEPTANCE.md。修正教案AI_JSON_STRICT_CONFIG_FIX_MANUAL_STEP.md的局部严格转换配置仅隔离编译/7探针通过，正式应用待用户保存后重建和针对性回归，不能标已修复。

2026-10-17公开推荐Controller手动课：既定POST /api/assistant/ask与question输入不变，Controller改调AssistantService.ask，200 data.keyword/maxPrice/answer/products结构保持，answer取Service模型理由/分支提示，products只为有效推荐编号对应的真实公开卡片，按推荐顺序。完整教案ASSISTANT_RECOMMENDATION_CONTROLLER_MANUAL_STEP.md，用户手动替换/尚未构建或运行本组；下一模拟两Python接口及真实Java推荐链集中验收。内部Product不直接返回，现有安全/400/503路径保持，文字事实评审待办不因接线消失。

2026-10-17Java推荐业务手动约定：AssistantService.ask复用已验收候选查询，空关键词/无候选直接提示；有候选映射AiCandidateProduct并保存id→Product快照，调用AiClient.recommend后按有效编号顺序取原商品，组合内部AssistantResult(intent,answer,products)，推荐为空时固定提示。完整教案ASSISTANT_RECOMMENDATION_SERVICE_MANUAL_STEP.md，由用户手写，尚未构建/联调；当前公共Controller仍旧searchCandidates，下节切换并映射公开卡片，不直接输出Product实体。

2026-10-17Java推荐客户端手动约定：AiClient.recommend(AiAskRequest) POST内部/api/ask，解析AiAskResponse.data为AiRecommendationData，复核理由非空白≤2000、编号列表非null≤5，编号逐个不得为null且必须属于本次请求候选，按返回顺序去重；合法空编号允许。异常复用RestClientException→503。教案JAVA_RECOMMENDATION_CLIENT_MANUAL_STEP.md，用户手写/未构建或运行，本组Service/公开响应衔接与独立验收下一步。

2026-10-17Java推荐响应手动约定：正常Python整体JSON映射AiAskResponse(AiRecommendationData data)，内层AiRecommendationData(String answer,List<Long> recommendedIds)，字段名与Python一致；AiAskRequest/AiCandidateProduct已只读确认用户写入。响应record本课仅用户新建教案，下一AiClient发送请求/解析/长度与成员复核，再Service与公开卡片衔接，尚未构建或验收Java推荐链。教案JAVA_RECOMMENDATION_RESPONSE_MANUAL_STEP.md，Python82项保持独立范围。

2026-10-17推荐Python验收补充：内部POST /api/ask当前用户已实现并重启生效；79模拟+1运行OpenAPI+2真实推荐共82检查通过，测试候选跑鞋1/2在跑鞋需求返回[1,2]，耳机需求返回[]与固定无适合提示。见RECOMMENDATION_ACCEPTANCE.md及verification/recommendation-http-2026-10-17.json。此次候选为提供的样例，Java推荐接线未实现；实际理由含无依据“缓震更好”比较，文字事实仍需收紧/评审，不把结构与编号检查当无幻觉保证。

2026-10-17推荐HTTP手动课：main.py计划新增内部POST /api/ask，AskRequest解析问题/候选，调用recommend_products并model_dump外包data返回200 answer/recommendedIds；原始请求无效由FastAPI默认422 detail，已知模型HTTP/输出格式/候选外编号异常返回503 error.code=AI_SERVICE_UNAVAILABLE、message=商品推荐暂时不可用，请稍后重试。完整教案RECOMMENDATION_API_MANUAL_STEP.md，用户手写/尚未运行验收；Java公共/api/assistant/ask仍未接此次推荐，下一Python验收再Java客户端/编号复核。

2026-10-17推荐函数手动约定：recommend_products(AskRequest)将每件候选介绍仅取前300字构造JSON提示，复用模型JSON模式，解析RecommendationResult，编号必须属于本次候选（不只是库中存在），按模型顺序去重；非法编号抛ValueError供后续/api/ask映射503。空编号返回固定无合适推荐提示与[]，有编号返回trim理由/有效编号；Java后续仍需复核并从真实候选快照组卡片。80字为提示词目标、schema硬上限2000，256token截断继续失败处理。教案RECOMMENDATION_SERVICE_MANUAL_STEP.md，本轮未代写/调用模型/验收，HTTP路由与Java接线下一步，原候选功能验收保持。

2026-10-17推荐schema手动约定：内部POST /api/ask输入AskRequest继承IntentRequest.question校验，products必须1～20件CandidateProduct(id正StrictInt、title非空白≤200、brand非空白≤100、price为0.00～99999999.99两位字符串、description字符串≤10000)。模型内层RecommendationResult.answer非空白≤2000、recommendedIds最多5个StrictInt，可空；后续代码必须验证候选成员并去重，Java再次复核后从真实候选快照组卡片。提示词对介绍取有限摘要避免全量长文本；无候选由Java直接处理。教案RECOMMENDATION_SCHEMA_MANUAL_STEP.md，仅用户手写指导，尚未注册/调用/验收本推荐功能，既有候选接口验收保持。

2026-10-17最新真实链补验：公共POST /api/assistant/ask，问题“我想买跑鞋，最高预算500元。”通过Java→真实Python/DeepSeek→真实商品库→Java，HTTP200 data.keyword跑鞋/maxPrice500.00/products为id1(299.00)、id2(459.00)，answer固定筛选提示。真实请求1次、补充检查12项通过，见verification/candidate-real-2026-10-17.json；本组之前“真实模型链待补”已覆盖。下一带候选推荐与编号复核尚待实现，单个样例不等于普适需求准确率或性能。

2026-10-17最新验收：当前Java40源码构建成功，POST /api/assistant/ask与GET /api/csrf已由用户实现，当前JAR+模拟需求上游+真实MySQL只读123断言通过。预算500跑鞋真实返回299/459两件，条件/卡片/固定提示、空关键词/无匹配、400/403/503、执行SQL与原管理保护实测，详见CANDIDATE_HTTP_ACCEPTANCE.md。当时Python8000未运行，未发真实模型请求，本组真实模型链待补；固定answer不当模型推荐理由，后续候选推荐约定保持。

2026-10-17公共安全手动课：明确permitAll GET /api/csrf、POST /api/assistant/ask，ADMIN与默认CSRF保持；公共GET返回data.token/headerName，调用方保留同会话Cookie并按headerName携token提交question，不要求管理员凭据。完整教案ASSISTANT_SECURITY_MANUAL_STEP.md；本轮只读配置与官方资料/维护文档，用户手写、未构建联调，下一集中验收。缺失/无效CSRF令牌仍403；permitAll不等于关闭CSRF。

2026-10-17候选HTTP响应手动课：沿既定POST /api/assistant/ask，IntentRequest.question→AssistantService.searchCandidates→AssistantResponse(keyword,maxPrice,answer,products)，外层data；products复用ProductSummaryResponse，价格两位字符串，最多20条ID升序。answer暂为固定业务提示：关键词空澄清/无匹配/筛选数量，不等于模型推荐理由。代码教案ASSISTANT_HTTP_RESPONSE_MANUAL_STEP.md，由用户手写；当前安全配置仍拒绝此未白名单入口，下一步精确放行与GET /api/csrf接线，保留CSRF及ADMIN规则，再集中验收。未构建/运行本组Java，不改变后续带候选推荐约定。

2026-10-16编排Service手动课：AssistantService.searchCandidates(question)返回内部CandidateSearchResult(intent,products)，提取一次后空关键词不查库，预算字符串/null转BigDecimal/null再查候选。下一外部响应需映射ProductSummaryResponse，不能直接暴露内部Product实体；补充需求/无匹配/有候选分别处理，先完成真实筛选里程碑再模型说明。本课仅教案、无新增HTTP入口或构建验收，见ASSISTANT_CANDIDATE_SERVICE_MANUAL_STEP.md。

2026-10-16候选查询手动课：ProductService.findCandidates(keyword,BigDecimal maxPrice)计划只查status=1且deleted=0、title/brand包含关键词、预算非null时price<=上限，最多20条ID升序；null预算不等于0，空关键词不查库并由后续业务提示补充。完整代码/未运行SQL语义示例见PRODUCT_CANDIDATE_QUERY_MANUAL_STEP.md。本轮仅用户手写指导，未实施/编译/执行查询，Java需求调用与业务编排待集中验收。

2026-10-15Java客户端手动约定：AiClient.parseIntent(question)解析AiIntentResponse，要求keyword非null且最长50，maxPrice null或规范的0.00～99999999.99两位小数字符串；关键词trim后返回AiIntentData。空关键词后续澄清，不查全库；非法上游响应按RestClientException→Java503。完整教案JAVA_INTENT_CLIENT_MANUAL_STEP.md，尚未编译/运行本轮新方法，商品候选查询仍待实现。

2026-10-15Java需求DTO手动准备：输入IntentRequest.question非空白/最长1000，Python完整响应AiIntentResponse.data为AiIntentData(keyword字符串/maxPrice金额字符串或null)。尚未写Java客户端/业务入口；DTO本课由用户新建，未构建验收。Java后续独立检查并转BigDecimal查询，不将null当0，空关键词后续引导补充。代码教案JAVA_INTENT_DTO_MANUAL_STEP.md；Python42项验收保持。

2026-10-15最新需求提取验收：用户已实现POST /api/intent，当前实际代码与模拟响应39项、OpenAPI1项和真实模型2项共42检查全部通过。真实question跑鞋最高500元→200 data.keyword跑鞋/maxPrice500.00；手机无最高预算→200手机/null。默认422、模型/输出异常503及普通聊天回归已验证，见INTENT_ACCEPTANCE.md与verification/intent-http-2026-10-15.json。以下“尚未实现/api/intent”均为历史；Java读取条件/查候选及/api/ask导购仍待实现，不能以两条真实样例当全部语义准确。

2026-10-15需求提取路由手动课：POST /api/intent输入question，正常200 data.keyword/maxPrice；默认原始输入422，模型HTTP/输出格式/金额异常503 error.code=AI_SERVICE_UNAVAILABLE、message=需求提取暂时不可用，请稍后重试。main.py新增路由完整教案见INTENT_API_MANUAL_STEP.md，尚未由助手实施或运行验收；后续Java和真实商品查询仍待做。

2026-10-15需求提取函数手动课补充：模型客户端新增可选JSON模式，提取结果由Pydantic解析；maxPrice为null或规范两位小数字符串，数值有限且在0～99999999.99，不接受需要舍入改变预算的精度（数值相同的尾零可规范）。用户question校验仍422；模型输出格式/金额异常在后续路由映射503。Java复核同一金额范围；无关键词后续澄清。当前仅代码教案，未代写/注册/api/intent或验收，见INTENT_EXTRACT_MANUAL_STEP.md。

2026-10-15阶段4启动设计：当前给intent_schema.py数据类手动课，内部POST /api/intent尚待实现。输入question非空白1～1000，输出data.keyword字符串最长50（允许空，空时后续业务引导补充，不直接无条件查全库）、maxPrice金额字符串或null。下一提取解析用Decimal校验规范金额，Java独立复核后构建查询，不将模型文本拼SQL。正式需求提取/导购/Web尚未实现；阶段3连通验收状态见第14节及MODEL_HTTP_ACCEPTANCE.md。

2026-10-15当前下一课：手动实现本机内部POST /api/model/chat，详细约定见本文件第14节及PYTHON_MODEL_API_MANUAL_STEP.md。本轮仅教案/约定，正式main.py尚未修改或验收；已有CLI成功保持，导购/api/ask仍候选。

2026-10-15最新状态：回退后用户已手写模型配置/客户端/测试入口，修复解释器与SSL后，用户提供model_probe真实中文回答及退出码0，首次CLI短文本成功，见PYTHON_MODEL_TROUBLESHOOTING.md。当前尚未实现模型FastAPI路由或Java模型HTTP入口；下一小范围约定并实现，导购协议仍候选。下方“模型文件均移除/首次真实调用未成功”等状态均为历史；CLI成功不能当模型HTTP接口已验收。

2026-10-15回退后当前状态：model_config/model_client/model_probe与新增HTTPX依赖均按用户要求移除。DeepSeek为后续历史选择，当前无模型实现或真实调用；下文此前“模块已实现/33项通过”仅回退前历史，不自动恢复。导购接口仍候选。

2026-10-15：用户已选择DeepSeek官方API。Python model_client.ask_model与本机model_probe脚本已实现，默认官方地址/deepseek-flash、thinking disabled、stream false、输出上限256token，用于第一笔短文本接入验证；33项模拟检查通过，真实Key/账号/API调用仍待本机验证。该CLI不新增HTTP接口；下方intent/ask/导购协议仍候选，JSON mode尚未实现。本节更早“服务商未确定”已由本更新覆盖。

用于手机第三册，按主路线逐章实践。服务商/模型/预算尚未确定，只有兼容Chat Completions且支持JSON mode的教学实现；没有真实收费调用或效果验收。

1. 前端→Java POST /api/assistant/ask：JSON {"question":"想买五百以内跑鞋"}，非空白最长1000；首版匿名只读导购，仍开启CSRF，GET /api/csrf取得对应会话令牌。管理商品仍需ADMIN。
2. Java→Python POST /api/intent：question，200 data.keyword（最长50）/maxPrice（金额字符串或null）。Python调用模型提取，Java复核范围；解析可能误解，页面展示识别条件供核对。
3. Java查MySQL status=1且未删除，按title/brand关键词与price<=maxPrice筛选，最多20件ID升序候选；无候选直接返回无匹配，不调用第二次推荐模型。
4. Java→Python POST /api/ask：question和products候选（id/title/brand/price/description），200 data.answer（最多2000）/recommendedIds（最多5）。Python校验编号必须属于候选；Java再次核对编号，去重后从数据库查询快照组商品卡片，模型不提供事实价格/图片。
5. Java→前端200 data.keyword/maxPrice/answer/products，products沿用ProductSummaryResponse字段。无匹配为空数组；400 INVALID_ARGUMENT，安全错误401/403对应统一JSON，Python/模型/协议失败503 AI_SERVICE_UNAVAILABLE。不把非法JSON、截断或虚构编号包装成成功。

Python三个模型配置只在服务端环境变量：MODEL_BASE_URL、MODEL_API_KEY、MODEL_NAME；不进前端、PDF或Git真实值。候选模型适配可换，Java商品业务与协议边界保留。两次HTTP的耗时需实测，当前不写准确率/性能数字。

Web候选先用Spring静态同域实验台，动态文本使用textContent，不执行模型HTML。正式React/Vite页面与复杂状态仍按vibe coding推进；身份/私有会话/SSE/购物车/RAG/部署仍后续阶段，不把首版单轮导购当完整Agent项目。

## 14. 阶段3内部模型连通接口（待手写与验收）

2026-10-15最新状态：Python POST /api/model/chat及Java管理员POST /api/admin/ai/chat已由用户手写完成并集中验收。Java32源码package，49项检查通过，其中一次当前Java→用户Python→官方模型→Java真实200。默认Python422、模型已知失败503、Java400/401/403/503与健康回声回归分别实测（故障用模拟上游/注入异常，真实模型请求仅一次）；证据MODEL_HTTP_ACCEPTANCE.md和verification/model-http-2026-10-15.json。以下“待手写/验收”仅历史，阶段4导购仍候选。

2026-10-15本课约定，正式代码未实施/运行，不替代阶段4带真实商品的/api/ask。

- 本机Python POST /api/model/chat，JSON {"message":"请用一句话介绍自己"}。message字符串1～1000字符、至少一个非空白字符；调用前strip两端空白。
- 成功200：{"data":{"answer":"模型回答","service":"shopagent-ai"}}。
- 字段缺失/null/空串/纯空白/超长/非法JSON：FastAPI/Pydantic默认422，detail数组，不发模型请求。不同于未来Java入口的400校验。
- 已知上游HTTP/网络/超时、配置ValueError与响应结构错误：503，{"error":{"code":"AI_SERVICE_UNAVAILABLE","message":"模型服务暂时不可用，请稍后重试"}}；原异常堆栈仅服务端日志。意外错误保留500。
- 普通同步def配现有同步HTTPX函数，不修改已成功客户端SSL配置；Java模型入口本课不实现。
- 运行入口须为Uvicorn进程设置DEEPSEEK_API_KEY；原CLI运行配置不会自动传递。下一重启/Swagger操作、集中核对200/422/503及健康/回声回归；未验证前不写已验收。

手动代码与解释：PYTHON_MODEL_API_MANUAL_STEP.md。

2026-10-15Java侧衔接约定（待手写/验收）：计划管理员POST /api/admin/ai/chat，JSON message使用ChatRequest校验；转发Python /api/model/chat，返回200 data.answer/service。AiChatResponse包AiChatData。沿用现有管理员认证与CSRF规则；输入400、依赖失败503。此课只给三个DTO代码，尚未新增Java客户端或入口。本轮只读确认Python OpenAPI中已注册模型POST，不证明真实HTTP已通过。
