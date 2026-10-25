from dataclasses import replace
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

from tests.support import OfflineMixin
from knowledge_loader import load_documents, document_fingerprint
from knowledge_scope import resolve_scope, has_supported_topic
from knowledge_service import answer_knowledge
from knowledge_splitter import split_knowledge
from fastapi.testclient import TestClient
from main import app


class CatalogScopeTests(OfflineMixin, unittest.TestCase):
    def setUp(self):
        super().setUp()
        self.documents = load_documents()
        self.e02 = next(d for d in self.documents if d.key == 'demo-audio-e02')

    def test_all_manuals_have_unique_scoped_chunks(self):
        chunks = [c for d in self.documents for c in split_knowledge(
            d.text, key=d.key, source=d.source, title=d.title, version=d.version)]
        self.assertEqual(len(self.documents), 37)
        self.assertEqual(len(chunks), 147)
        self.assertEqual(len({c['id'] for c in chunks}), 147)
        self.assertTrue(all('资料版本' in c['document'] for c in chunks))

    def test_similar_models_resolve_separately(self):
        for model in ('E01', 'E02', 'R01', 'K01', 'M01', 'B01', 'C01', 'K1'):
            key, reason = resolve_scope(model + '怎么使用', self.documents)
            self.assertIsNone(reason, model)
            self.assertTrue(key.endswith(model.lower().replace('k1', 'k1')))

    def test_unknown_ambiguous_and_partial_models_do_not_guess(self):
        for question in ('支持无线充电吗', 'E01和E02哪个好', 'E010支持无线充电吗', 'X99怎么用'):
            key, reason = resolve_scope(question, self.documents)
            self.assertIsNone(key)
            self.assertTrue(reason)

    def test_database_identity_is_exact_and_question_cannot_switch_product(self):
        args = (self.e02.title, self.e02.brand)
        self.assertEqual(resolve_scope('支持无线充电吗', self.documents, *args), (self.e02.key, None))
        self.assertIsNone(resolve_scope('E01支持无线充电吗', self.documents, *args)[0])
        self.assertIsNone(resolve_scope('怎么用', self.documents, self.e02.title + '新版', self.e02.brand)[0])
        self.assertIsNone(resolve_scope('怎么用', self.documents, self.e02.title, '其他品牌')[0])

    def test_unsupported_topics_are_not_retrieved(self):
        self.assertTrue(has_supported_topic('支持无线充电吗', self.e02))
        self.assertFalse(has_supported_topic('E02保修多少年', self.e02))

    def test_document_or_metadata_change_invalidates_fingerprint(self):
        baseline = document_fingerprint(self.documents)
        for field, value in (('text', self.e02.text + '\n修改'), ('version', 'new'), ('brand', 'new')):
            changed = [replace(d, **{field: value}) if d.key == self.e02.key else d for d in self.documents]
            self.assertNotEqual(baseline, document_fingerprint(changed))

    def test_manifest_cannot_escape_knowledge_directory(self):
        with tempfile.TemporaryDirectory() as folder:
            manifest = {'products': [{'key': 'demo-escape', 'title': '演示', 'brand': '演示', 'source': '../outside.md'}]}
            Path(folder, 'catalog.json').write_text(json.dumps(manifest), encoding='utf-8')
            with self.assertRaises(ValueError):
                load_documents(Path(folder))

    def test_clarification_does_not_call_retrieval_or_paid_model(self):
        with patch('knowledge_service.search_knowledge') as search, patch('knowledge_service.ask_model') as model:
            self.assertFalse(answer_knowledge('支持无线充电吗').answerable)
            self.assertFalse(answer_knowledge('E01怎么充电', self.e02.title, self.e02.brand).answerable)
            search.assert_not_called()
            model.assert_not_called()

    def test_wrong_product_chunk_is_rejected_before_model(self):
        wrong = {'chunk': {'product_key': 'demo-audio-e01'}}
        with patch('knowledge_service.search_knowledge', return_value=[wrong]), patch('knowledge_service.ask_model') as model:
            with self.assertRaises(ValueError):
                answer_knowledge('怎么充电', self.e02.title, self.e02.brand)
            model.assert_not_called()

    def test_http_rejects_partial_identity_and_arbitrary_source(self):
        with TestClient(app) as client, patch('main.answer_knowledge') as answer:
            for body in ({'question': '怎么充电', 'productTitle': self.e02.title},
                         {'question': '怎么充电', 'source': '../secrets/key'},
                         {'question': '怎么充电', 'productTitle': self.e02.title, 'productBrand': ''}):
                self.assertEqual(client.post('/api/knowledge/ask', json=body).status_code, 422)
            answer.assert_not_called()

    def test_http_passes_selected_identity_and_versioned_sources(self):
        from knowledge_schema import KnowledgeResult
        expected = KnowledgeResult(answer='资料不足', answerable=False, sources=[])
        with TestClient(app) as client, patch('main.answer_knowledge', return_value=expected) as answer:
            response = client.post('/api/knowledge/ask', json={'question': '怎么充电',
                'productTitle': self.e02.title, 'productBrand': self.e02.brand})
            self.assertEqual(response.status_code, 200)
            answer.assert_called_once_with('怎么充电', self.e02.title, self.e02.brand)
