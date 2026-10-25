import unittest
from unittest.mock import patch
import httpx
from tests.support import OfflineMixin, candidate
from fastapi.testclient import TestClient
from main import app


class HttpContractTests(OfflineMixin, unittest.TestCase):
    def setUp(self):
        super().setUp()
        self.client = TestClient(app)
        self.addCleanup(self.client.close)

    def test_health_contract(self):
        response = self.client.get("/health")
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["data"]["service"], "shopagent-ai")

    def test_blank_or_oversized_question_is_422_without_model(self):
        with patch("main.ask_model") as model:
            for message in (" ", "x" * 1001):
                with self.subTest(message_length=len(message)):
                    response = self.client.post("/api/model/chat", json={"message": message})
                    self.assertEqual(response.status_code, 422)
            model.assert_not_called()

    def test_model_failure_returns_controlled_503(self):
        with patch("main.ask_model", side_effect=httpx.ReadTimeout("fixture")):
            response = self.client.post("/api/model/chat", json={"message": "你好"})
        self.assertEqual(response.status_code, 503)
        self.assertEqual(response.json()["error"]["code"], "AI_SERVICE_UNAVAILABLE")

    def test_unknown_product_from_model_is_503_not_success(self):
        with patch("selection_service.ask_model", return_value='{"recommendedIds":[999]}'):
            response = self.client.post("/api/recommendations/select", json={"question": "耳机", "products": [candidate()]})
        self.assertEqual(response.status_code, 503)

    def test_cart_endpoint_rejects_extra_command(self):
        response = self.client.post("/api/cart/intent", json={"question": "第一款设为2", "products": [], "execute": True})
        self.assertEqual(response.status_code, 422)
