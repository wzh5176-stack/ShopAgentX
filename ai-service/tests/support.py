"""在独立测试进程中隔离网络和重量级检索依赖。"""
import sys
import types
from unittest.mock import Mock, patch

# 只替换检索边界；knowledge_service 的引用校验和业务代码仍真实执行。
# 完整向量模型和检索质量应由单独的 RAG 集成验收覆盖。
search_stub = types.ModuleType("knowledge_search")
search_stub.search_knowledge = Mock(side_effect=AssertionError("test must provide retrieval fixture"))
sys.modules["knowledge_search"] = search_stub


class OfflineMixin:
    def setUp(self):
        super().setUp()
        # 阻断真实 HTTP 传输，保留 Windows asyncio 创建内部 socketpair 的能力。
        # TestClient 的进程内 ASGI transport 不受影响。
        for target in ("httpx.HTTPTransport.handle_request", "httpx.AsyncHTTPTransport.handle_async_request"):
            guard = patch(target, side_effect=AssertionError("offline regression forbids outbound HTTP"))
            guard.start()
            self.addCleanup(guard.stop)


def candidate():
    return {"id": 9, "title": "测试耳机", "brand": "Demo", "price": "19.90", "description": "仅测试"}
