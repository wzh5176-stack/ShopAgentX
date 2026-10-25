// Real Edge + real static files; API responses are fixtures. No business service is started.
const fs = require('node:fs/promises');
const path = require('node:path');
const assert = require('node:assert/strict');
const { chromium } = require(process.env.SHOPAGENT_PLAYWRIGHT_MODULE || 'playwright');
const root = path.resolve(__dirname, '..');
const web = path.join(root, 'backend/src/main/resources/static');
const output = path.join(root, 'tmp/catalog-rag/ui');
const types = {'.html':'text/html; charset=utf-8','.js':'text/javascript; charset=utf-8',
  '.css':'text/css; charset=utf-8','.svg':'image/svg+xml','.png':'image/png','.ttf':'font/ttf'};

(async () => {
  await fs.mkdir(output, {recursive:true});
  const catalog = JSON.parse(await fs.readFile(path.join(root, 'ai-service/knowledge/catalog.json'), 'utf8'));
  const products = catalog.products.map((p,i) => ({id:1001+i, availableQuantity:20, ...p}));
  const e02 = products.find(p => p.model === 'E02');
  const browser = await chromium.launch({headless:true,
    ...(process.env.SHOPAGENT_EDGE_PATH ? {executablePath:process.env.SHOPAGENT_EDGE_PATH} : {})});
  const report = {passed:false, scope:'real Edge/static files with fixture APIs; not a live deployment', checks:[]};
  try {
    for (const width of [390, 1440]) {
      const context = await browser.newContext({viewport:{width,height:900}, deviceScaleFactor:1});
      const page = await context.newPage();
      const errors = [], bodies = [];
      page.on('pageerror', e => errors.push(e.message));
      await context.route('**/*', async route => {
        const url = new URL(route.request().url());
        const json = (data, status=200) => route.fulfill({status,contentType:'application/json',body:JSON.stringify({data})});
        if (url.origin !== 'http://shopagent.test') return route.abort();
        if (url.pathname === '/api/auth/me') return json(null,401);
        if (url.pathname === '/api/csrf') return json({token:'fixture', headerName:'X-CSRF-TOKEN'});
        if (url.pathname === '/api/products') {
          const keyword=url.searchParams.get('keyword') || '', size=Number(url.searchParams.get('size') || 10), current=Number(url.searchParams.get('page') || 1);
          const selected=products.filter(p => (p.title+p.brand).includes(keyword));
          return json({items:selected.slice((current-1)*size,current*size),page:current,size,total:selected.length,totalPages:Math.ceil(selected.length/size)});
        }
        if (url.pathname.startsWith('/api/products/')) return json(products.find(p=>p.id===Number(url.pathname.split('/').pop())));
        if (url.pathname === '/api/assistant/knowledge') {
          bodies.push(route.request().postDataJSON());
          const b=bodies.at(-1);
          if (b.question.includes('保修')) return json({answer:'现有资料不足以回答这个问题。',answerable:false,sources:[]});
          return json({answer:'演示资料：E02支持USB-C或无线充电。', answerable:true, sources:[{
            chunkId:e02.key+':2',source:e02.source,document:e02.title+' · 资料版本 '+e02.version+' · 虚构演示',
            section:'供电与续航', content:b.question.includes('转义') ? '<img src=x onerror=alert(1)>为原文显示测试。' : 'USB-C或无线充电；单次约7小时，配盒约28小时。'}]});
        }
        if (!url.pathname.startsWith('/shopagent/')) return route.fulfill({status:404,body:''});
        const file=path.resolve(web,'.'+decodeURIComponent(url.pathname));
        if (!file.startsWith(web+path.sep)) return route.fulfill({status:400,body:''});
        try {return route.fulfill({status:200,contentType:types[path.extname(file)]||'application/octet-stream',body:await fs.readFile(file)});}
        catch {return route.fulfill({status:404,body:''});}
      });
      await page.goto('http://shopagent.test/shopagent/products.html');
      await page.waitForSelector('.goods-card');
      assert.equal(await page.locator('#goods-page').innerText(),'第 1 / 5 页');
      await page.locator('#goods-next').click();
      await page.waitForSelector('.goods-card[data-product-id="1009"]');
      assert.equal(await page.locator('#goods-page').innerText(),'第 2 / 5 页');
      await page.goto('http://shopagent.test/shopagent/products.html?keyword=澄声');
      await page.waitForSelector('.goods-card');
      assert.equal(await page.locator('.goods-card').count(),8);
      assert.equal(await page.locator('[data-filter]').count(),7);
      assert.equal(await page.locator('#goods-pagination').isHidden(),true);
      await page.locator('.goods-picture img.photo').first().scrollIntoViewIfNeeded();
      await page.waitForFunction(()=>document.querySelector('.goods-picture img.photo')?.naturalWidth>0);
      report.checks.push({width, name:'category filter and local illustrations',passed:true});
      await page.screenshot({path:path.join(output,`products-${width}.png`),fullPage:true});
      await page.goto(`http://shopagent.test/shopagent/product.html?id=${e02.id}`);
      await page.waitForSelector('#product-content:not([hidden])');
      assert.equal(await page.locator('#detail-knowledge').getAttribute('href'),`./knowledge.html?productId=${e02.id}`);
      await page.locator('#detail-knowledge').click();
      await page.waitForFunction(id=>document.getElementById('knowledge-product').value===String(id),e02.id);
      assert.equal(await page.locator('#knowledge-product option').count(),37);
      await page.locator('#knowledge-question').fill('支持无线充电吗');
      await page.locator('#knowledge-submit').click();
      await page.waitForSelector('#knowledge-result:not([hidden])');
      assert.equal(bodies.at(-1).productId,e02.id);
      await page.screenshot({path:path.join(output,`knowledge-${width}.png`),fullPage:true});
      const overflow=await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth+1);
      assert.equal(overflow,false,'knowledge page horizontal overflow');
      await page.locator('#knowledge-question').fill('E02转义测试');
      await page.locator('#knowledge-submit').click();
      await page.waitForSelector('#knowledge-result:not([hidden])');
      assert.equal(await page.locator('#knowledge-sources img').count(),0);
      assert.ok((await page.locator('#knowledge-sources').innerText()).includes('<img src=x'));
      await page.locator('#knowledge-product').selectOption('1001');
      assert.equal(await page.locator('#knowledge-result').isHidden(),true);
      await page.locator('[data-knowledge-question]').first().click();
      assert.equal(await page.locator('#knowledge-product').inputValue(),'');
      await page.locator('#knowledge-question').fill('E02保修多少年？');
      await page.locator('#knowledge-submit').click();
      await page.waitForSelector('#knowledge-result:not([hidden])');
      assert.equal(await page.locator('.knowledge-source').count(),0);
      assert.equal(await page.locator('#knowledge-source-heading').isHidden(),true);
      assert.deepEqual(errors,[]);
      report.checks.push({width,name:'detail identity, CSRF POST, source escaping, stale citation clearing, refusal and no JS errors',passed:true});
      await context.close();
    }
    report.passed=true;
  } finally {
    await browser.close();
    await fs.writeFile(path.join(output,'summary.json'),JSON.stringify(report,null,2));
  }
  console.log(JSON.stringify(report,null,2));
})().catch(e=>{console.error(e);process.exitCode=1;});
