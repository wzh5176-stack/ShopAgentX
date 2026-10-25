import unittest
from unittest.mock import patch
import httpx
from tests.support import OfflineMixin
from knowledge_service import answer_knowledge, NO_EVIDENCE_ANSWER
from stream_events import model_stream_events


def hit():
    return {"score": 0.8, "chunk": {"id": "fixture:1", "source": "fixture.md",
            "product_key": "demo-earphone-k1", "document": "演示耳机", "section": "充电", "content": "仅支持USB-C。"}}


class KnowledgeTests(OfflineMixin, unittest.TestCase):
    def test_no_evidence_does_not_call_model(self):
        with patch("knowledge_service.search_knowledge", return_value=[]), patch("knowledge_service.ask_model") as model:
            result = answer_knowledge("K1能无线充电吗")
            self.assertFalse(result.answerable)
            self.assertEqual(result.answer, NO_EVIDENCE_ANSWER)
            model.assert_not_called()

    def test_only_real_retrieved_citations_are_returned(self):
        with patch("knowledge_service.search_knowledge", return_value=[hit()]), patch("knowledge_service.ask_model",
                return_value='{"answer":"仅支持USB-C","answerable":true,"citedIds":["fixture:1","fixture:1"]}'):
            result = answer_knowledge("K1怎么充电")
            self.assertEqual(len(result.sources), 1)
            self.assertEqual(result.sources[0].source, "fixture.md")

    def test_fabricated_citation_is_rejected(self):
        with patch("knowledge_service.search_knowledge", return_value=[hit()]), patch("knowledge_service.ask_model",
                return_value='{"answer":"支持无线充电","answerable":true,"citedIds":["invented"]}'), self.assertRaises(ValueError):
            answer_knowledge("K1怎么充电")

    def test_answer_without_citation_is_rejected(self):
        with patch("knowledge_service.search_knowledge", return_value=[hit()]), patch("knowledge_service.ask_model",
                return_value='{"answer":"支持","answerable":true,"citedIds":[]}'), self.assertRaises(ValueError):
            answer_knowledge("K1怎么充电")


class StreamTests(OfflineMixin, unittest.TestCase):
    def test_success_has_deltas_then_done(self):
        with patch("stream_events.ask_model_stream", return_value=iter_generator(["你好", "耳机"])):
            events = list(model_stream_events("推荐"))
        self.assertIn("event: delta", events[0])
        self.assertIn("event: done", events[-1])
        self.assertIn("你好耳机", events[-1])

    def test_midstream_failure_has_error_and_never_done(self):
        def failing():
            yield "部分答案"
            raise httpx.ReadTimeout("fixture timeout")
        with patch("stream_events.ask_model_stream", return_value=failing()):
            events = list(model_stream_events("推荐"))
        self.assertIn("event: error", events[-1])
        self.assertFalse(any("event: done" in event for event in events))

    def test_cancel_closes_upstream_generator(self):
        closed = []
        def upstream():
            try:
                yield "部分答案"
                yield "后续答案"
            finally:
                closed.append(True)
        with patch("stream_events.ask_model_stream", return_value=upstream()):
            stream = model_stream_events("推荐")
            next(stream)
            stream.close()
        self.assertEqual(closed, [True])


def iter_generator(parts):
    yield from parts
