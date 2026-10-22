# ShopAgent：一次导购请求的六步完整流程

整理日期：2026-10-22。对应当前 D:\ShopAgent-X 源码。

这份文档按执行顺序，把聊天中的六次复盘合成一份。代码均为现有实现的关键节选，用于阅读，不需要重新新建同名类或替换代码。示例会话编号、消息编号、JSON和SQL用于说明，不是本次执行结果。

## 先看六步地图

| 步骤 | 要解决的问题 | 主要入口 |
| --- | --- | --- |
| 1 | 网页发了什么，Java在哪里接收？ | workspace.js、ChatMessageStreamController |
| 2 | 对话属于谁，模型能看到哪些历史？ | ChatMessageService.loadContext |
| 3 | 一句话怎样变成搜索条件？ | AiClient.parseConversationIntent、conversation_service.py |
| 4 | 商品从哪里来，模型能选哪些商品？ | ProductService.findCandidates、selection_service.py |
| 5 | 回答怎样逐段显示？ | model_stream_client.py、SseEmitter、workspace.js |
| 6 | 什么时刻才算保存成功，刷新怎样恢复？ | ChatMessageWriteService.saveTurn、历史接口 |

~~~text
网页输入
  → Java接请求
  → 检查身份/对话，读取历史
  → Python提取需求
  → Java查询真实候选，Python选择编号
  → Python生成解释，Java转发，网页显示预览
  → Java事务保存，发送业务done
  → 后续从MySQL读取历史
~~~

这是正常能进入商品推荐分支的一条主线。需要澄清、没有商品或发生错误时，会进入相应分支。

## 第1步：网页请求和Java入口

### 用户操作怎样变成请求

假设当前对话编号为12，用户发送“推荐五百元以内的跑鞋”。[workspace.js](../backend/src/main/resources/static/shopagent/workspace.js) 中的 streamGuide 发出：

~~~http
POST /api/conversations/12/messages/stream
Content-Type: application/json
Accept: text/event-stream
~~~

~~~json
{
  "content": "推荐五百元以内的跑鞋"
}
~~~

浏览器通过当前8080入口请求Nginx，Nginx转交一个Java实例。请求还按网页代码携带同源会话Cookie与CSRF请求头；认证细节保留为单独复盘主题。

### Controller在哪里接收

[ChatMessageStreamController.java](../backend/src/main/java/com/wwzyhy/shopagent/controller/ChatMessageStreamController.java)：

~~~java
@PostMapping("/api/conversations/{conversationId}/messages/stream")
public ResponseEntity<SseEmitter> stream(
        @PathVariable("conversationId") Long conversationId,
        @Valid @RequestBody MessageSendRequest request,
        Authentication authentication) {

    SseEmitter emitter =
            streamService.stream(authentication, conversationId, request);
    return ResponseEntity.ok()
            .contentType(MediaType.TEXT_EVENT_STREAM)
            .header("Cache-Control", "no-cache")
            .header("X-Accel-Buffering", "no")
            .body(emitter);
}
~~~

| 参数 | 从哪里来 |
| --- | --- |
| conversationId | URL中的12 |
| request | JSON正文转换成的Java对象 |
| authentication | Spring Security提供的当前登录身份 |

身份不是网页在JSON里填写一个userId就能决定的。Controller接收参数，把业务交给Service，返回持续发送事件的SseEmitter响应通道。

[MessageSendRequest.java](../backend/src/main/java/com/wwzyhy/shopagent/dto/MessageSendRequest.java)：

~~~java
public record MessageSendRequest(
        @NotBlank(message = "消息不能为空")
        @Size(max = 1000, message = "消息最多1000个字符")
        String content
) {
}
~~~

request.content()可以取出消息。record可先理解为装数据的Java类型；@Valid配合字段注解检查消息非空白、长度不超过1000。

## 第2步：检查归属，读取历史和版本

### 先查这个对话是否属于当前用户

[ChatMessageService.java](../backend/src/main/java/com/wwzyhy/shopagent/service/ChatMessageService.java) 的loadContext先调用：

~~~java
chatConversationService.getOwned(authentication, conversationId);
~~~

[CurrentUserService.java](../backend/src/main/java/com/wwzyhy/shopagent/service/CurrentUserService.java) 根据可信身份中的用户名查出有效AppUser的数据库id；[ChatConversationService.java](../backend/src/main/java/com/wwzyhy/shopagent/service/ChatConversationService.java) 再同时限制：

~~~java
query.eq("id", id);
query.eq("user_id", userId);
~~~

假设对话12、用户3，条件相当于：

~~~sql
SELECT *
FROM chat_conversations
WHERE id = 12 AND user_id = 3;
~~~

id是对话表的编号，user_id是对话所属用户。查不到就抛ConversationNotFoundException，不继续读该对话消息；对话不存在或不属于本人使用同一类未找到结果。

### 只取模型需要的最近消息

loadContext的查询：

~~~java
QueryWrapper<ChatMessage> query = new QueryWrapper<>();
query.select("id", "role", "content");
query.eq("conversation_id", conversationId);
query.orderByDesc("id");

Page<ChatMessage> pageRequest = new Page<>(1, 10, false);
Page<ChatMessage> result =
        chatMessageMapper.selectPage(pageRequest, query);
~~~

它读取当前对话、按id从新到旧、最多10条消息；false表示不另查总条数。一句用户消息和一句助手消息各算一条，不是10轮。

先从倒序列表首条记下lastMessageId，空历史记0，然后：

~~~java
Collections.reverse(records);
~~~

把顺序改为旧→新，再逐条构造AiConversationMessage(role, content)，供模型正常阅读。

最终返回的[ConversationContext.java](../backend/src/main/java/com/wwzyhy/shopagent/dto/ConversationContext.java)包含：

~~~java
public record ConversationContext(
        List<AiConversationMessage> history,
        long lastMessageId
) {
}
~~~

history是发给模型的历史文字；lastMessageId留在Java，用于第6步并发检查，不是消息条数，不传给Python。

## 第3步：Python结合历史提取需求

### Java发送的不是裸文本

Java在ChatMessageStreamService中构造：

~~~java
new AiConversationIntentRequest(content, context.history())
~~~

对应JSON字段是question和history。当前问题单独放question，history里是已保存的过去消息。

[AiClient.java](../backend/src/main/java/com/wwzyhy/shopagent/service/AiClient.java) 的parseConversationIntent：

~~~java
AiConversationIntentResponse response = client.post()
        .uri("/api/conversations/intent")
        .body(request)
        .retrieve()
        .body(AiConversationIntentResponse.class);
~~~

当前容器配置的基址是http://ai:8000。Java发HTTP请求给Python，Spring把对象序列化为JSON，并把响应反序列化为Java对象。

### Python先校验，再调用模型

[main.py](../ai-service/main.py)的对应入口是POST /api/conversations/intent。FastAPI/Pydantic校验结构，交给[conversation_service.py](../ai-service/conversation_service.py)：

~~~python
answer = ask_model(prompt, json_mode=True)
intent = ConversationIntent.model_validate_json(answer)
normalize_intent(intent)
~~~

prompt包含规则、当前问题和历史。ask_model调用DeepSeek；model_validate_json解析并校验输出；normalize_intent规范搜索条件。要求JSON输出仍需要程序校验，不能把格式要求当成模型绝不出错的保证。

以历史“500元内跑鞋”、本轮“预算改成三百元呢？”为例，期望结果：

~~~json
{
  "data": {
    "keyword": "跑鞋",
    "maxPrice": "300.00",
    "clarification": null
  }
}
~~~

| 字段 | 含义 |
| --- | --- |
| keyword | 品类或品牌关键词 |
| maxPrice | 单件最高预算字符串；null表示未设置上限 |
| clarification | 需要追问的问题；null表示可以继续搜索 |

规则要求当前明确的新预算优先，只改预算时沿用用户明确的品类；换品类不自动继承旧预算；“便宜点”但无明确金额时追问。助手提到的商品价格不能直接当用户预算。

Python用data包装结果，Java取response.data()并再次检查字段。若需要澄清，Java先形成追问答复，不进入商品推荐查询分支。

## 第4步：Java查真实候选，模型只选择编号

### 用已有意图查库

[AssistantService.java](../backend/src/main/java/com/wwzyhy/shopagent/service/AssistantService.java)的searchCandidates(AiIntentData intent)：

~~~java
BigDecimal maxPrice = intent.maxPrice() == null
        ? null
        : new BigDecimal(intent.maxPrice());

List<Product> products = productService.findCandidates(
        intent.keyword(), maxPrice);
~~~

预算字符串转BigDecimal金额数值。这里用已经得到的intent，不重复把question送去提取一遍。

### Wrapper就是条件的组装器

[ProductService.java](../backend/src/main/java/com/wwzyhy/shopagent/service/ProductService.java) 的findCandidates：

~~~java
QueryWrapper<Product> query = new QueryWrapper<>();
query.eq("status", 1);
query.and(group -> group
        .like("title", searchKeyword)
        .or()
        .like("brand", searchKeyword));

if (maxPrice != null) {
    query.le("price", maxPrice);
}
query.orderByAsc("id");

Page<Product> pageRequest = new Page<>(1, 20, false);
return productMapper.selectPage(pageRequest, query).getRecords();
~~~

eq表示等于，like表示包含关键词，le表示小于等于。and包住标题或品牌条件，避免OR影响上架/预算条件。Product实体@TableLogic自动补逻辑未删除条件。

“跑鞋、最高300元”的查询效果相当于：

~~~sql
SELECT *
FROM products
WHERE deleted = 0
  AND status = 1
  AND (title LIKE '%跑鞋%' OR brand LIKE '%跑鞋%')
  AND price <= 300.00
ORDER BY id ASC
LIMIT 20;
~~~

最多取前20个候选，不是让模型在数据库外任意生成商品，也不是这条链路使用向量库搜索商品。

### 为什么准备DTO和Map两份结构

[AssistantStreamService.java](../backend/src/main/java/com/wwzyhy/shopagent/service/AssistantStreamService.java)把候选转换成AiCandidateProduct，交给Python，同时建立：

~~~java
productsById.put(product.getId(), product);
~~~

DTO提供模型需要的编号、名称、品牌、价格和描述；Map保留“编号→原Product对象”，之后按模型选出的编号取回真实候选。

AiClient.selectProducts向POST /api/recommendations/select发送question、products、searchIntent。searchIntent带着合并历史后的条件，解决“预算改成300”省略品类的问题。

[selection_service.py](../ai-service/selection_service.py)要求模型只返回：

~~~json
{
  "recommendedIds": [1]
}
~~~

最多5个编号，不在这一步生成推荐理由。Python与Java都检查编号属于候选并去重，Java再通过Map取回Product：

~~~java
Product product = productsById.get(id);
if (product == null) {
    throw new RestClientException("选择结果没有对应的真实商品");
}
~~~

商品名称、价格等来自这次数据库候选。没有候选或选择结果为空时返回提示，不补造商品。第3步理解需求、第4步选择商品是两个职责不同的模型调用。

## 第5步：解释流经过Python、Java到网页

### 对已经选好的商品生成说明

Java通过POST /api/recommendations/explain/stream发送已选择的商品和条件。[explanation_service.py](../ai-service/explanation_service.py)要求只解释这些商品，不重新选择、不补充未提供的库存、优惠或性能事实。

[model_stream_client.py](../ai-service/model_stream_client.py)的模型请求设置stream为True，用httpx.stream持续读文字。片段大小不固定，不保证一个片段就是一个字。

### Python包装事件

[stream_events.py](../ai-service/stream_events.py)：

~~~python
for piece in stream:
    parts.append(piece)
    yield encode_event("delta", {"content": piece})
~~~

parts保留完整回答所需的片段；yield逐次把事件交给StreamingResponse。事件格式类似：

~~~text
event: delta
data: {"content":"可以考虑"}

event: delta
data: {"content":"轻量跑鞋A。"}

~~~

event表示类型，data是JSON，空行分隔事件。

### Java累积并转发

AssistantStreamService.readExplanation处理delta：

~~~java
AiStreamDelta data = codec.readDelta(event.data());
answer.append(data.content());
onDelta.accept(data);
~~~

answer拼完整文字，onDelta通知外层立即发送片段。实际回调job::delta可理解为data -> job.delta(data)。最终[ChatMessageStreamService.java](../backend/src/main/java/com/wwzyhy/shopagent/service/ChatMessageStreamService.java)使用：

~~~java
emitter.send(
        SseEmitter.event()
                .name(event)
                .data(data, MediaType.APPLICATION_JSON)
);
~~~

通过SseEmitter发给浏览器。代码还检查长度、未知事件和完整结束标记。Nginx配置关闭代理缓冲，并设置适合流的超时。

### 网页只是先显示预览

workspace.js读取响应流，按事件分帧、解析JSON，再执行：

~~~javascript
answer += value.content;
onDelta(answer);
~~~

外层回调：

~~~javascript
target.textContent = answer;
~~~

屏幕于是逐段更新。代码保留pending处理网络块之间的残缺事件；一次网络读取不保证恰好一个完整事件。

必须区分：

| 事件方向 | done表示什么 |
| --- | --- |
| Python→Java | 模型解释已完整生成 |
| Java→网页 | 本轮消息已完成数据库保存 |

Java不会把Python的done直接当保存成功事件透传。文字片段显示出来，不等于消息已落库；error事件或提前断线不能当成功完成。

## 第6步：检查版本，事务保存，发送done，恢复历史

### 生成在事务外，最终写入用短事务

ChatMessageStreamService在拿到完整结果后：

~~~java
job.beginCommit();
MessageSendResponse saved = writeService.saveTurn(
        authentication, conversationId, content,
        recommendation, context.lastMessageId());
job.success(saved);
~~~

[ChatMessageWriteService.java](../backend/src/main/java/com/wwzyhy/shopagent/service/ChatMessageWriteService.java)的saveTurn是独立Service上的@Transactional方法。模型与网络等待不放在这个写入事务里。

### 锁住会话，再读取当前版本

先按本人会话加锁：

~~~java
query.eq("id", conversationId);
query.eq("user_id", userId);
query.last("FOR UPDATE");
~~~

直到事务结束，其他对相同记录加锁的写入需等待。然后用最新消息查询的LIMIT 1 FOR SHARE读取当前id，与开始生成时的版本比较：

~~~java
if (actualLastMessageId != expectedLastMessageId) {
    throw new ConversationChangedException();
}
~~~

示意：

~~~text
A、B都根据最新消息50生成
A先保存用户51、助手52
B准备保存：期望50，实际52
B拒绝本轮写入，提示刷新
~~~

这些编号只是示意。当前SSE连接已开始时，冲突通过error事件的CONVERSATION_CHANGED通知网页；前端内部映射409，不是服务器重新把已开始的HTTP流改成409响应。

### 用户消息和助手消息一起保存

事务中：

~~~java
ChatMessage userMessage =
        insertMessage(conversationId, "user", content, null);

ChatMessage assistantMessage =
        insertMessage(
                conversationId,
                "assistant",
                recommendation.answer(),
                recommendationJson
        );
~~~

| 记录 | 保存文字 | 推荐JSON |
| --- | --- | --- |
| user | 用户本轮输入 | null |
| assistant | 完整回答 | 序列化后的推荐条件/商品等 |

同一事务还更新会话updated_at。这里保存失败等运行时异常会回滚这组修改，避免只保存一半。方法返回MessageSendResponse(userMessage, assistantMessage, recommendation)。

### 提交后才发送业务done

Spring事务代理在正常返回给调用者前完成提交，然后调用者执行job.success(saved)。它发送：

~~~java
emit("done", saved);
emitter.complete();
~~~

网页校验保存后的消息对，mergeMessages并显示“这次对话已保存”。此处的done携带数据库生成的消息编号等结果，不只是“模型说完了”。

若提交后网络断开，数据库仍可能已保存，网页未收到done时应查历史核实；停止显示不能撤销已经提交的消息。

### 刷新从哪里恢复

网页选择对话或刷新时请求：

~~~http
GET /api/conversations/12/messages?size=20
~~~

[ChatConversationController.java](../backend/src/main/java/com/wwzyhy/shopagent/controller/ChatConversationController.java)调用ChatMessageService.readHistory：先核本人归属，再从MySQL分页读取消息，解recommendation_json恢复商品卡。

网页默认一次20条是展示分页；模型最多最近10条是上下文限制。二者都不表示删除数据库中的旧记录，也不会为了刷新历史重新调用模型生成回答。

## 把六步连成一次讲述

可以先用自己的话讲清这段，再逐个定位源码：

“用户发送需求后，Java接收请求，确认会话属于当前用户并读取最近历史。Python结合历史提取关键词和预算，Java据此查询MySQL真实商品，再由模型从候选里选择编号。选定商品交给模型生成解释，Python和Java逐段把文字送到网页。完整生成后，Java检查会话版本，在短事务里成对保存问题、回答与推荐数据，提交后发送完成事件。刷新时从数据库读回历史和商品卡。”

## 经常混淆的点

| 问题 | 当前项目的答案 |
| --- | --- |
| 模型记住了之前对话吗？ | Java从数据库读最近历史，再随本轮问题发送 |
| Python在这条链里直接查商品库吗？ | 商品查询由Java完成 |
| 模型返回价格就可以展示吗？ | 展示商品对象来自数据库候选，选择编号要校验 |
| 最近10条等于只保存10条吗？ | 不等于，只限制模型本轮上下文 |
| 一个文字片段等于一个字吗？ | 不保证，网络块也不等于事件 |
| 两个done是一回事吗？ | Python是生成完成，Java是提交保存完成 |
| 看到回答就一定保存了吗？ | 文字可能仍是预览，保存结果看业务done或历史 |
| 没收到done就肯定没存吗？ | 提交后断线仍可能已存，应查询历史 |
| lastMessageId是消息数量吗？ | 是生成前读取到的最新消息编号，用于版本检查 |
| 事务会一直等模型生成吗？ | 模型在写入事务外，最后的写入才进入短事务 |

## 已完成什么，下一步做什么

截至本次归档，约定的首个本地可演示版本已有商品/账号/导购/多轮流式/资料问答/购物车功能、本地六服务部署和验收记录；独立RedisCluster演练、SQL隔离恢复与商品接口轻量实测均已归档。六步导购主线已经讲完，不代表学习者必须立刻全部掌握。

范围和证据详见[项目首页](../README.md)、[运行手册](RUNTIME_RUNBOOK.md)、[实测报告](PRODUCT_BASELINE_REPORT.md)。订单支付、地址物流、Android和LangGraph图编排未实现；网站仍用原单节点共享会话Redis，实验Cluster独立。

本次只读发现本地main分支尚无任何提交，origin配置为git@github.com:wwzyhy/ShopAgent.git；只说明远端地址已配置，不表示本轮已验证GitHub认证或上传。下一项交付收尾建议是按手动教学方式核对待提交文件/忽略规则，再完成首次Git提交与目标仓库推送；本次没有执行暂存、提交或推送。

技术复盘可继续独立的“购物车待确认→用户确认→事务执行”链，之后是RAG以及此前暂缓的Cookie/会话/CSRF。它们不是把本轮六步无限往后追加，也不要求再增加新业务功能。

