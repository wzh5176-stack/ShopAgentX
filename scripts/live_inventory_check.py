"""Explicit live acceptance: creates then logically deletes one clearly labeled test product; no orders/payments."""
import argparse
import base64
from http.cookiejar import CookieJar
import json
import os
from pathlib import Path
from urllib.error import HTTPError
from urllib.request import build_opener, HTTPCookieProcessor, Request
import uuid

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base-url', default='http://127.0.0.1:8080')
    parser.add_argument('--admin-password-env', default='SHOPAGENT_ADMIN_PASSWORD')
    parser.add_argument('--model', action='store_true', help='make one real model request using configured service')
    parser.add_argument('--output', default='tmp/inventory/live.json')
    args = parser.parse_args()
    password = os.environ.get(args.admin_password_env)
    if not password: raise RuntimeError('Admin password environment missing; never pass it in arguments')
    auth = 'Basic ' + base64.b64encode(('shopagent_admin:' + password).encode()).decode()
    opener = build_opener(HTTPCookieProcessor(CookieJar()))
    def call(method, path, body=None, authenticated=True):
        headers={'Accept':'application/json'}
        if authenticated: headers['Authorization']=auth
        if method != 'GET':
            _, csrf = call('GET','/api/csrf',authenticated=authenticated)
            headers[csrf['data']['headerName']]=csrf['data']['token']
        if body is not None: headers['Content-Type']='application/json'
        request=Request(args.base_url+path, headers=headers, method=method,
                        data=json.dumps(body,ensure_ascii=False).encode() if body is not None else None)
        try:
            with opener.open(request, timeout=120) as response:
                raw = response.read()
                return response.status,json.loads(raw) if raw else {}
        except HTTPError as error:
            try: content=json.load(error)
            except ValueError: content={}
            return error.code,content
    report={'passed':False,'scope':'real deployed Java/MySQL/Redis/Python via nginx; no payment calls','checks':{}}
    product_id=None
    try:
        status, catalog=call('GET','/api/products?size=50')
        assert status==200
        products=catalog['data']['items']; assert all(type(p['availableQuantity']) is int for p in products)
        e02=next(p for p in products if 'E02' in p['title'])
        report['checks']['public_inventory_and_catalog']=True
        body={'title':'库存闭环验收演示-'+uuid.uuid4().hex[:8],'brand':'验收演示','price':'1.00',
              'imageUrl':'/shopagent/catalog/e01.svg','description':'虚构验收商品，完成后逻辑删除。','status':1}
        status, created=call('POST','/api/admin/products',body); assert status==201
        product_id=created['data']['id']
        _, inventory=call('GET',f'/api/admin/products/{product_id}/inventory')
        assert inventory['data']['availableQuantity']==0
        report['checks']['new_product_zero_stock']=True
        # A fresh unauthenticated opener has no saved admin session or Basic header.
        try:
            build_opener().open(args.base_url+f'/api/admin/products/{product_id}/inventory')
            raise AssertionError('Anonymous admin inventory unexpectedly allowed')
        except HTTPError as error: assert error.code==401
        report['checks']['anonymous_admin_denied']=True
        replenish={'requestKey':uuid.uuid4().hex,'quantity':7,'reason':'部署验收补货'}
        first_status, first=call('POST',f'/api/admin/products/{product_id}/inventory/replenishments',replenish)
        again_status, again=call('POST',f'/api/admin/products/{product_id}/inventory/replenishments',replenish)
        assert first_status==again_status==200 and first['data']['id']==again['data']['id']
        _, current=call('GET',f'/api/admin/products/{product_id}/inventory')
        assert current['data']['availableQuantity']==7 and current['data']['reservedQuantity']==0
        status,_=call('POST',f'/api/admin/products/{product_id}/inventory/replenishments',{**replenish,'quantity':8});assert status==409
        _, log=call('GET',f'/api/admin/products/{product_id}/inventory/replenishments')
        assert len(log['data']['items'])==1 and log['data']['items'][0]['actor']=='shopagent_admin'
        report['checks']['restock_replay_conflict_and_audit']=True
        _, detail=call('GET',f'/api/products/{product_id}');assert detail['data']['availableQuantity']==7
        report['checks']['public_stock_after_restock']=True
        if args.model:
            status, response=call('POST','/api/assistant/knowledge',{'question':'支持无线充电吗？','productId':e02['id']})
            assert status==200 and response['data']['answerable'] is True, f'Live RAG failed, status={status}'
            sources=response['data']['sources'];assert sources and all(s['document'].startswith(e02['title']+' · 资料版本 ') for s in sources)
            assert any('无线充电' in s['content'] for s in sources)
            report['checks']['real_deepseek_scoped_rag']=True
            report['rag_answer']=response['data']['answer']
        status, response=call('POST','/api/assistant/knowledge',{'question':'保修多少年？','productId':e02['id']})
        assert status==200 and response['data']['answerable'] is False and response['data']['sources']==[]
        report['checks']['unsupported_question_refused']=True
        report['passed']=True
    finally:
        if product_id is not None:
            status,_=call('DELETE',f'/api/admin/products/{product_id}')
            report['test_product_id']=product_id;report['test_product_logically_deleted']=status in (200,204)
            report['passed']=report['passed'] and report['test_product_logically_deleted']
        output=Path(args.output);output.parent.mkdir(parents=True,exist_ok=True)
        output.write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
        print(json.dumps(report,ensure_ascii=False,indent=2))
    return 0 if report['passed'] else 1

if __name__=='__main__': raise SystemExit(main())
