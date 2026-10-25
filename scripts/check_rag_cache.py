"""本地向量缓存边界测试；需要完整RAG环境中的numpy，不下载模型或请求LLM。"""
from dataclasses import replace
from pathlib import Path
import sys
import tempfile
import types
import unittest
from unittest.mock import Mock, patch
import numpy as np

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'ai-service'))
import knowledge_index as index
from knowledge_loader import load_documents


class RagCacheTests(unittest.TestCase):
    def setUp(self):
        self.folder = tempfile.TemporaryDirectory()
        self.addCleanup(self.folder.cleanup)
        self.addCleanup(setattr, index, '_SNAPSHOT', None)
        self.patch = patch.object(index, 'CACHE_DIR', Path(self.folder.name))
        self.patch.start()
        self.addCleanup(self.patch.stop)
        index._SNAPSHOT = None
        def normalized_vectors(texts):
            vectors = np.zeros((len(texts), 512), dtype='float32')
            vectors[:, 0] = 1
            return vectors
        self.embedding = Mock(side_effect=normalized_vectors)
        module = types.ModuleType('embedding_client')
        module.embed_texts = self.embedding
        self.modules = patch.dict(sys.modules, {'embedding_client': module})
        self.modules.start()
        self.addCleanup(self.modules.stop)

    def test_memory_and_disk_reuse_avoid_reembedding(self):
        first = index.knowledge_snapshot()
        self.assertIs(first, index.knowledge_snapshot())
        index._SNAPSHOT = None
        second = index.knowledge_snapshot()
        self.assertEqual(first[0], second[0])
        self.assertEqual(self.embedding.call_count, 1)

    def test_document_revision_builds_new_cache(self):
        first = index.knowledge_snapshot()
        docs = load_documents()
        docs[0] = replace(docs[0], version='changed-version')
        with patch.object(index, 'load_documents', return_value=docs):
            second = index.knowledge_snapshot()
        self.assertNotEqual(first[0], second[0])
        self.assertEqual(self.embedding.call_count, 2)

    def test_corrupted_cache_is_rebuilt(self):
        first = index.knowledge_snapshot()
        (index.CACHE_DIR / f'knowledge-{first[0]}.npz').write_bytes(b'corrupted cache')
        index._SNAPSHOT = None
        self.assertEqual(index.knowledge_snapshot()[3].shape, (147, 512))
        self.assertEqual(self.embedding.call_count, 2)

    def test_invalid_vector_space_is_rejected(self):
        for vectors in (np.zeros((147, 512)), np.ones((147, 5)), np.full((147, 512), np.nan)):
            self.embedding.side_effect = None
            self.embedding.return_value = vectors
            with self.assertRaises(ValueError):
                index.knowledge_snapshot()

    def test_unwritable_cache_falls_back_to_memory(self):
        # CACHE_DIR points to a regular file; mkdir raises, but retrieval can still use memory.
        blocked = Path(self.folder.name, 'not-a-directory')
        blocked.write_text('fixture')
        with patch.object(index, 'CACHE_DIR', blocked):
            snapshot = index.knowledge_snapshot()
            self.assertEqual(snapshot[3].shape, (147, 512))


if __name__ == '__main__':
    unittest.main(verbosity=2)
