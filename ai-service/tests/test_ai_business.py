import json
import unittest
from unittest.mock import patch
from tests.support import OfflineMixin, candidate
from intent_schema import SearchIntent
from intent_service import normalize_intent
from selection_service import select_products
from recommendation_service import recommend_products
from recommendation_schema import AskRequest
from cart_tool_schema import CartToolIntentRequest
from cart_tool_service import extract_cart_tool_intent
from conversation_schema import ConversationIntentRequest
from conversation_service import extract_conversation_intent


class IntentTests(OfflineMixin, unittest.TestCase):
    def test_budget_is_decimal_and_normalized(self):
        result = normalize_intent(SearchIntent(keyword=" 耳机 ", maxPrice="19.9"))
        self.assertEqual((result.keyword, result.maxPrice), ("耳机", "19.90"))

    def test_no_budget_remains_none(self):
        self.assertIsNone(normalize_intent(SearchIntent(keyword="耳机")).maxPrice)

    def test_invalid_budgets_are_rejected(self):
        for amount in ("-1", "NaN", "Infinity", "abc", "1.001", "100000000"):
            with self.subTest(amount=amount), self.assertRaises(ValueError):
                normalize_intent(SearchIntent(keyword="耳机", maxPrice=amount))

    def test_missing_category_requires_clarification(self):
        request = ConversationIntentRequest(question="便宜一点", history=[])
        with patch("conversation_service.ask_model", return_value='{"keyword":"","maxPrice":null,"clarification":null}'):
            self.assertIsNotNone(extract_conversation_intent(request).clarification)


class RecommendationTests(OfflineMixin, unittest.TestCase):
    def setUp(self):
        super().setUp()
        self.request = AskRequest(question="推荐耳机", products=[candidate()])

    def test_model_cannot_invent_product(self):
        with patch("selection_service.ask_model", return_value='{"recommendedIds":[999]}'), self.assertRaises(ValueError):
            select_products(self.request)

    def test_duplicate_selection_is_collapsed(self):
        with patch("selection_service.ask_model", return_value='{"recommendedIds":[9,9]}'):
            self.assertEqual(select_products(self.request).recommendedIds, [9])

    def test_recommendation_also_checks_candidate_ids(self):
        with patch("recommendation_service.ask_model", return_value='{"answer":"推荐","recommendedIds":[999]}'), self.assertRaises(ValueError):
            recommend_products(self.request)

    def test_empty_selection_uses_controlled_answer(self):
        with patch("recommendation_service.ask_model", return_value='{"answer":"编造的结论","recommendedIds":[]}'):
            result = recommend_products(self.request)
            self.assertEqual(result.recommendedIds, [])
            self.assertNotEqual(result.answer, "编造的结论")

    def test_bool_is_not_product_id(self):
        with patch("selection_service.ask_model", return_value='{"recommendedIds":[true]}'), self.assertRaises(ValueError):
            select_products(self.request)


class CartToolTests(OfflineMixin, unittest.TestCase):
    def setUp(self):
        super().setUp()
        self.request = CartToolIntentRequest(question="第一款设为2", products=[{
            "title": "测试耳机", "brand": "Demo", "price": "19.90"}])

    def extract(self, data):
        with patch("cart_tool_service.ask_model", return_value=json.dumps(data)):
            return extract_cart_tool_intent(self.request)

    def test_valid_operation_is_only_a_proposal(self):
        intent = self.extract({"action": "set_quantity", "productIndex": 1, "quantity": 2})
        self.assertEqual((intent.productIndex, intent.quantity), (1, 2))

    def test_out_of_list_product_is_rejected(self):
        with self.assertRaises(ValueError):
            self.extract({"action": "set_quantity", "productIndex": 2, "quantity": 2})

    def test_non_execution_cannot_carry_parameters(self):
        for action in ("none", "clarify"):
            with self.subTest(action=action), self.assertRaises(ValueError):
                self.extract({"action": action, "productIndex": 1, "quantity": 2})

    def test_invalid_quantity_or_extra_field_is_rejected(self):
        for quantity in (True, "2", 0, 100, None):
            with self.subTest(quantity=quantity), self.assertRaises(ValueError):
                self.extract({"action": "set_quantity", "productIndex": 1, "quantity": quantity})
        with self.assertRaises(ValueError):
            self.extract({"action": "none", "productIndex": None, "quantity": None, "execute": True})
