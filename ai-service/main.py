from cart_tool_schema import CartToolIntentRequest
from cart_tool_service import extract_cart_tool_intent

import logging

from recommendation_schema import AskRequest
from recommendation_service import recommend_products

from selection_service import select_products

import httpx
from fastapi import FastAPI
from fastapi.responses import JSONResponse, StreamingResponse
from pydantic import BaseModel, Field

from stream_events import model_stream_events

from model_client import ask_model

logger = logging.getLogger(__name__)
app = FastAPI(title="ShopAgent AI Service")

from intent_schema import IntentRequest
from intent_service import extract_intent

from knowledge_service import answer_knowledge
from knowledge_schema import KnowledgeRequest

from conversation_schema import ConversationIntentRequest
from conversation_service import extract_conversation_intent

from explanation_schema import RecommendationExplainRequest
from explanation_service import build_explanation_prompt


@app.get("/health")
def health():
    return {
        "data": {
            "status": "UP",
            "service": "shopagent-ai"
        }
    }

class EchoRequest(BaseModel):
    message: str = Field(min_length=1, max_length=1000)


@app.post("/api/echo")
def echo(request: EchoRequest):
    return {
        "data": {
            "message": request.message,
            "service": "shopagent-ai"
        }
    }

class ChatRequest(BaseModel):
    message: str = Field(
        min_length=1,
        max_length=1000,
        pattern=r"\S"
    )


@app.post("/api/model/chat")
def chat(request: ChatRequest):
    try:
        answer = ask_model(request.message.strip())
    except (httpx.HTTPError, ValueError, KeyError, IndexError, TypeError):
        logger.exception("模型调用失败")
        return JSONResponse(
            status_code=503,
            content={
                "error": {
                    "code": "AI_SERVICE_UNAVAILABLE",
                    "message": "模型服务暂时不可用，请稍后重试"
                }
            }
        )

    return {
        "data": {
            "answer": answer,
            "service": "shopagent-ai"
        }
    }

@app.post("/api/intent")
def parse_intent(request: IntentRequest):
    try:
        intent = extract_intent(request.question.strip())
    except (httpx.HTTPError, ValueError, KeyError, IndexError, TypeError):
        logger.exception("需求提取失败")
        return JSONResponse(
            status_code=503,
            content={
                "error": {
                    "code": "AI_SERVICE_UNAVAILABLE",
                    "message": "需求提取暂时不可用，请稍后重试"
                }
            }
        )

    return {"data": intent.model_dump()}

@app.post("/api/ask")
def ask(request: AskRequest):
    try:
        result = recommend_products(request)
    except (httpx.HTTPError, ValueError, KeyError, IndexError, TypeError):
        logger.exception("商品推荐失败")
        return JSONResponse(
            status_code=503,
            content={
                "error": {
                    "code": "AI_SERVICE_UNAVAILABLE",
                    "message": "商品推荐暂时不可用，请稍后重试"
                }
            }
        )

    return {"data": result.model_dump()}


@app.post("/api/knowledge/ask")
def knowledge_ask(request: KnowledgeRequest):
    try:
        result = answer_knowledge(request.question.strip(), request.productTitle, request.productBrand)
    except (httpx.HTTPError, OSError, ValueError, KeyError, IndexError, TypeError):
        logger.exception("知识问答失败")
        return JSONResponse(
            status_code=503,
            content={
                "error": {
                    "code": "AI_SERVICE_UNAVAILABLE",
                    "message": "知识问答暂时不可用，请稍后重试"
                }
            }
        )

    return {"data": result.model_dump()}

@app.post("/api/conversations/intent")
def conversation_intent(request: ConversationIntentRequest):
    try:
        intent = extract_conversation_intent(request)
    except (httpx.HTTPError, ValueError, KeyError, IndexError, TypeError):
        logger.exception("对话需求提取失败")
        return JSONResponse(
            status_code=503,
            content={
                "error": {
                    "code": "AI_SERVICE_UNAVAILABLE",
                    "message": "对话需求提取暂时不可用，请稍后重试"
                }
            }
        )

    return {"data": intent.model_dump()}

@app.post("/api/model/chat/stream")
def chat_stream(request: ChatRequest):
    return StreamingResponse(
        model_stream_events(request.message.strip()),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "X-Accel-Buffering": "no"
        }
    )

@app.post("/api/recommendations/select")
def select_recommendations(request: AskRequest):
    try:
        selection = select_products(request)
    except (httpx.HTTPError, ValueError, KeyError, IndexError, TypeError):
        logger.exception("商品选择失败")
        return JSONResponse(
            status_code=503,
            content={
                "error": {
                    "code": "AI_SERVICE_UNAVAILABLE",
                    "message": "商品选择暂时不可用，请稍后重试"
                }
            }
        )

    return {"data": selection.model_dump()}

@app.post("/api/recommendations/explain/stream")
def explain_recommendations(request: RecommendationExplainRequest):
    try:
        prompt = build_explanation_prompt(request)
    except (ValueError, KeyError, IndexError, TypeError):
        logger.exception("推荐说明准备失败")
        return JSONResponse(
            status_code=503,
            content={
                "error": {
                    "code": "AI_SERVICE_UNAVAILABLE",
                    "message": "推荐说明暂时不可用，请稍后重试"
                }
            }
        )

    return StreamingResponse(
        model_stream_events(prompt),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "X-Accel-Buffering": "no"
        }
    )

@app.post("/api/cart/intent")
def cart_intent(request: CartToolIntentRequest):
    try:
        intent = extract_cart_tool_intent(request)
    except (httpx.HTTPError, ValueError, KeyError, IndexError, TypeError):
        logger.exception("购物车意图识别失败")
        return JSONResponse(
            status_code=503,
            content={
                "error": {
                    "code": "AI_SERVICE_UNAVAILABLE",
                    "message": "购物车意图识别暂时不可用，请稍后重试"
                }
            }
        )

    return {"data": intent.model_dump()}
