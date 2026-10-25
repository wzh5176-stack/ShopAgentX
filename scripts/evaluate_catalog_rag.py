"""真实本地BGE/FAISS检索评测；不调用付费大模型，不等同生成答案质量评测。"""
import argparse
import json
import os
from pathlib import Path
import sys
from time import perf_counter
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'ai-service'))
os.environ['HF_HUB_OFFLINE'] = '1'
os.environ['TRANSFORMERS_OFFLINE'] = '1'

from knowledge_loader import load_documents
from knowledge_search import search_knowledge
from knowledge_index import knowledge_snapshot
from knowledge_scope import resolve_scope
from knowledge_service import answer_knowledge

QUERIES = {
    'audio': [('支持蓝牙连接吗', '连接与兼容'), ('怎么充电，续航多久', '供电与续航')],
    'shoes': [('尺码范围和适合的用途是什么', '尺码与用途'), ('可以机洗吗，怎么清洁', '清洁与维护')],
    'keyboard': [('有多少键，用什么方式连接', '配列与连接'), ('电池和充电方式是什么', '供电与充电')],
    'mouse': [('怎么连接，DPI是多少', '连接与参数'), ('电池供电和充电方式是什么', '供电与充电')],
    'bag': [('容量多大，能放多少英寸电脑', '容量与收纳'), ('可以机洗吗，怎么清洁', '清洁与限制')],
    'cup': [('容量多大，内胆什么材质', '容量与材质'), ('保温多久，能用微波炉吗', '温度与使用')],
}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', default=str(ROOT / 'tmp/catalog-rag/retrieval.json'))
    args = parser.parse_args()
    started = perf_counter()
    catalog = json.loads((ROOT / 'ai-service/knowledge/catalog.json').read_text(encoding='utf-8'))
    documents = load_documents()
    signature, _, chunks, vectors = knowledge_snapshot()
    results = []
    for item in catalog['products']:
        for question, expected_section in QUERIES[item['category']]:
            hits = search_knowledge(question, product_key=item['key'])
            sections = [h['chunk']['section'] for h in hits]
            scoped = bool(hits) and all(h['chunk']['product_key'] == item['key'] for h in hits)
            results.append({'model': item['model'], 'question': question, 'expected_section': expected_section,
                            'sections': sections, 'in_top3': expected_section in sections,
                            'top1': bool(sections) and sections[0] == expected_section, 'scoped': scoped})
    negatives = []
    for question in ('支持无线充电吗', 'E01和E02选哪个', 'E010怎么充电', 'X99怎么充电'):
        key, reason = resolve_scope(question, documents)
        negatives.append({'question': question, 'passed': key is None and bool(reason)})
    for item in catalog['products']:
        hits = search_knowledge('这件商品保修多少年', product_key=item['key'])
        negatives.append({'model': item['model'], 'question': '保修多少年', 'passed': not hits})
    e02 = next(d for d in documents if d.key == 'demo-audio-e02')
    with patch('knowledge_service.ask_model', side_effect=AssertionError('No paid model calls')):
        for question, title, brand in [('E01支持无线充电吗', e02.title, e02.brand),
                                        ('支持无线充电吗', e02.title + '新版', e02.brand)]:
            result = answer_knowledge(question, title, brand)
            negatives.append({'question': question, 'passed': not result.answerable and not result.sources})
    # 真实检索+受控生成结果，用来核对source/ID版本边界；不冒充真实LLM生成验收。
    hits = search_knowledge('E02支持无线充电吗')
    cite = next(h['chunk'] for h in hits if h['chunk']['section'] == '供电与续航')
    with patch('knowledge_service.ask_model', return_value=json.dumps({
            'answer': '演示资料记载E02支持USB-C或无线充电。', 'answerable': True, 'citedIds': [cite['id']]})):
        answer = answer_knowledge('E02支持无线充电吗')
        citation_ok = answer.answerable and answer.sources[0].source == e02.source and '资料版本' in answer.sources[0].document
    result = {'passed': all(r['in_top3'] and r['scoped'] for r in results)
                        and all(r['passed'] for r in negatives) and citation_ok,
              'catalog_version': catalog['version'], 'documents': len(documents), 'chunks': len(chunks),
              'vector_shape': list(vectors.shape), 'fingerprint': signature,
              'positive_cases': len(results), 'recall_at_3': sum(r['in_top3'] for r in results) / len(results),
              'top1_accuracy': sum(r['top1'] for r in results) / len(results),
              'scope_violations': sum(not r['scoped'] for r in results),
              'negative_cases': len(negatives), 'negative_passes': sum(r['passed'] for r in negatives),
              'citation_boundary_with_stub_model': bool(citation_ok), 'real_llm_calls': 0,
              'elapsed_seconds': round(perf_counter() - started, 2), 'positives': results, 'negatives': negatives}
    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding='utf-8')
    print(json.dumps({k: v for k, v in result.items() if k not in {'positives', 'negatives'}}, ensure_ascii=False, indent=2))
    return 0 if result['passed'] else 1


if __name__ == '__main__':
    raise SystemExit(main())
