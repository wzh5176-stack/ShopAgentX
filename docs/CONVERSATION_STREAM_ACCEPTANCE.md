# 多轮上下文与流式商品导购接线验收

2026-10-19，用户明确授权助手直接完成本次接线并概括说明。正式98 Java源码package成功，新增业务方法21项检查和实际HTTP/数据库89项检查全部通过。构建日志：tmp/assistant-stream-business/implementation-build.log。证据：verification/assistant-stream-business-formal-2026-10-19.json、verification/conversation-stream-formal-2026-10-19.json。

## 功能及文件

- AssistantService：原searchCandidates(AiIntentData)由private改public，复用条件查询，不重复提取意图。
- 新AssistantStreamService：真实候选→模型选择编号→按顺序取回真实商品→只传已选事实生成推荐理由；复用AiStreamJsonCodec，验证累计文本、完整结束和错误，最后返回AssistantResult。
- 新ChatMessageStreamService：先查本人会话并读取最近10条历史和最新消息id，再在原有界线程池中提取合并条件；处理澄清/无匹配或调用商品流；最后用原saveTurn短事务版本复核与成对保存。模型等待不持有写事务。
- 新ChatMessageStreamController：POST /api/conversations/{conversationId}/messages/stream，沿用MessageSendRequest。
- SecurityConfig：仅为该POST增加ROLE_USER匹配，保留CSRF、本人归属检查和管理员诊断接口原有规则。

Python上一组选择和解释端点保持，正式Python业务源码本轮未修改；未加入Agent/购物车或扩展到完整电商订单。现有网页仍保留原交互，本次交付为后端完整会话流链路；前端学习暂停，未将网页认作已接入新会话流。

## 对外协议

请求：登录USER在对应Cookie会话中携带CSRF头，Accept: text/event-stream，Content-Type: application/json，正文{"content":"改成400以内"}。路径编号必须属于本人。

- delta：{"content":"新增文字"}。属于临时预览，不能据此认定已保存。
- done：完整MessageSendResponse，含userMessage、assistantMessage、recommendation；仅在saveTurn事务代理返回、已提交后发送。消息包含实际数据库id，商品卡片来自Java真实商品。它和内部Python模型done的结构/含义不同，Python done不会直接转发。
- error：{"code":"...","message":"..."}，为安全固定文案。模型错误AI_SERVICE_UNAVAILABLE；会话版本冲突CONVERSATION_CHANGED；也处理会话失效、账号失效、数据库或内部故障。流打开后HTTP可能已200，不能再改成HTTP409/503；原非流式接口原有状态语义保持。
- 澄清、无候选或无选中商品，直接发送固定文字delta，再成对保存并done；跳过不必要的模型步骤。
- 开流前非法输入400、未认证401、无USER/无CSRF403、他人或不存在会话404、任务饱和503，均为JSON响应。

## 取消和提交边界

每个请求独立RUNNING/COMMITTING/FINISHED/CANCELLED状态。超时、关闭回调或已检测的发送失败，在提交开始前停止工作并阻止写入；显式关闭Java→Python流InputStream。阶段间与事件回调检查线程中断，队列有界。

beginCommit检查通过后进入短事务提交阶段；随后接收端断开/超时不再中断该数据库事务，以免将已经保存的记录误报为未保存。可能出现接收端未收到done但问答已经提交，此时应通过历史接口找回，不能无条件重发。测试已在真实MySQL用父会话锁刻意阻塞提交，验证超时后释放锁仍成对提交且历史可读。

不声称立即检测一切浏览器断连：无新发送时可能直到超时才感知。Python→真实模型HTTP的即时取消传播尚未证明；受控提供方门闩在测试中显式释放以完成回收。没有新增显式取消HTTP端点或刷新重试幂等协议。

## 本次验证

21项正式业务检查：真实商品映射顺序/精简事实、无匹配分支、异常与缺done/结束不一致拒绝成功、累计长度、回调和任务中断。

89项实际HTTP/真实MySQL检查：

- 当前正式JAR→实际Uvicorn/main/业务schema与模型协议解析→受控provider；仅模型边界替身，付费模型请求0。
- USER/管理员/匿名/CSRF/归属、无效参数；非法请求不调用模型。
- 首个delta在提供方释放前到达，预览时消息数为0，模型等待不锁父会话；done中的实际消息id对应已保存user/assistant对及recommendation_json。
- 历史传递、预算条件传到商品过滤、真实商品价格与顺序、最近10条窗口、澄清/无候选/空选择跳过不必要模型；旧非流式发送、健康和商品接口回归。
- 意图失败、非法选品编号、流中断均error且无消息；同版本并发一成功一冲突且只保存一对。
- 同一有界执行器12条接收任务/第13条503，已接收请求通过版本保护仅一对成功。
- 提交前超时零消息；已开始提交时超时不打断短事务、结果可从历史找回；复用原短事务异常回滚检查。
- app_users/products及测试范围外chat_conversations/chat_messages的基线摘要保持不变，未改权限、账号或商品。

真实DeepSeek多轮语义和流式效果仍待：本次终端只检查密钥是否存在，结果为无，未读取密钥内容/IDE私有配置，不以受控模型输出当语义效果。前端新会话流展示也未在本次验证。

## 测试数据和运行状态

新增声明测试会话21～26（标题前缀learn_stream_661715e2），保留20条验收消息77～96，均属于本次测试会话；失败和提交前超时会话为空。事务回滚和失败插入可产生自增号空洞，未执行清理DELETE。

所有助手临时Java/Python/数据库探针进程已停止；正常8000/8888无监听，MySQL7888/3306保持。未读或修改凭据文件，没有全局环境/依赖变更。复现入口tmp/conversation-stream-audit/audit.py及其Python边界替身、受限DB探针，均为本机验收工具，并非正式API。

下一按主路线进入Agent/购物车的最小业务场景，必要网页接线由助手承担；真实模型效果在已配置密钥的项目运行环境复验。首版进度仍按交付范围粗估，不因检查数机械增加。
