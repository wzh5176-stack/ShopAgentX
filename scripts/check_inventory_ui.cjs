const fs=require('node:fs/promises'), path=require('node:path'), assert=require('node:assert/strict');
const {chromium}=require(process.env.SHOPAGENT_PLAYWRIGHT_MODULE||'playwright');
const root=path.resolve(__dirname,'..'), web=path.join(root,'backend/src/main/resources/static');
const types={'.html':'text/html; charset=utf-8','.js':'text/javascript; charset=utf-8','.css':'text/css; charset=utf-8','.svg':'image/svg+xml','.ttf':'font/ttf','.png':'image/png'};
(async()=>{
 const out=path.join(root,'tmp/inventory/ui'); await fs.mkdir(out,{recursive:true});
 const browser=await chromium.launch({headless:true,...(process.env.SHOPAGENT_EDGE_PATH?{executablePath:process.env.SHOPAGENT_EDGE_PATH}:{})});
 const results=[];
 try {for(const width of [390,1440]){
  const context=await browser.newContext({viewport:{width,height:900}}), page=await context.newPage();
  let actor=null, stock=0, accepted=null; const posts=[], errors=[]; page.on('pageerror',e=>errors.push(e.message));
  await context.route('**/*',async route=>{
   const u=new URL(route.request().url()), reply=(data,status=200)=>route.fulfill({status,contentType:'application/json',body:JSON.stringify({data})});
   if(u.origin!=='http://shopagent.test')return route.abort();
   if(u.pathname==='/api/auth/me')return reply(actor?{username:actor}:null,actor?200:401);
   if(u.pathname==='/api/cart')return reply({items:[],totalQuantity:0,totalAmount:'0.00'});
   if(u.pathname==='/api/csrf')return reply({token:'fixture',headerName:'X-CSRF-TOKEN'});
   if(u.pathname==='/api/products/17')return reply({id:17,title:'库存闭环测试商品',brand:'演示',price:'19.90',description:'虚构演示商品',imageUrl:'/shopagent/catalog/e01.svg',availableQuantity:0});
   if(u.pathname==='/api/products')return reply({items:[],total:0,totalPages:0,page:1,size:8});
   if(u.pathname.startsWith('/api/admin/')){
    if(actor!=='admin')return reply(null,403);
    if(u.pathname==='/api/admin/products')return reply({items:[{id:17,title:'库存闭环测试商品',status:1}],totalPages:1});
    if(u.pathname.endsWith('/inventory'))return reply({productId:17,availableQuantity:stock,reservedQuantity:2});
    if(u.pathname.endsWith('/replenishments')){
     if(route.request().method()==='GET')return reply({items:accepted?[accepted]:[]});
     const body=route.request().postDataJSON(); posts.push(body);
     if(!accepted){stock+=body.quantity; accepted={id:1,productId:17,requestKey:body.requestKey,quantity:body.quantity,reason:body.reason,actor:'admin',availableQuantity:stock,reservedQuantity:2,createdAt:'2026-10-25T12:00:00'};return route.abort();}
     assert.equal(body.requestKey,accepted.requestKey);return reply(accepted);
    }
   }
   if(!u.pathname.startsWith('/shopagent/'))return route.fulfill({status:404,body:''});
   const file=path.resolve(web,'.'+u.pathname);if(!file.startsWith(web+path.sep))return route.abort();
   try{return route.fulfill({contentType:types[path.extname(file)]||'application/octet-stream',body:await fs.readFile(file)});}catch{return route.fulfill({status:404,body:''});}
  });
  await page.goto('http://shopagent.test/shopagent/inventory.html');
  await page.waitForSelector('#inventory-login:not([hidden])');
  actor='ordinary';await page.reload();await page.waitForFunction(()=>document.getElementById('inventory-status').textContent.includes('不是管理员'));
  assert.equal(await page.locator('#inventory-panel').isHidden(),true);
  actor='admin';await page.reload();await page.waitForSelector('#inventory-panel:not([hidden])');
  await page.locator('#inventory-reason').fill('测试补货');await page.locator('#inventory-submit').click();
  await page.waitForFunction(()=>document.getElementById('inventory-feedback').textContent.includes('尚未确认'));
  assert.equal(stock,10);assert.equal(await page.locator('#inventory-quantity').isDisabled(),true);
  await page.reload();await page.waitForSelector('#inventory-panel:not([hidden])');
  await page.waitForFunction(()=>document.getElementById('inventory-submit').textContent.includes('重试'));
  await page.locator('#inventory-submit').click();await page.waitForFunction(()=>document.getElementById('inventory-feedback').textContent.includes('补货已确认'));
  assert.equal(posts.length,2);assert.deepEqual(posts[0],posts[1]);assert.equal(stock,10);
  assert.ok((await page.locator('#inventory-stock').innerText()).includes('预占 2'));
  assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth+1),false);
  await page.screenshot({path:path.join(out,`inventory-${width}.png`),fullPage:true});
  await page.goto('http://shopagent.test/shopagent/product.html?id=17');await page.waitForSelector('#product-content:not([hidden])');
  assert.equal(await page.locator('#detail-save').isDisabled(),true);assert.equal(await page.locator('#detail-save').innerText(),'暂时缺货');
  assert.deepEqual(errors,[]);results.push({width,passed:true,checks:['anonymous login','ordinary user refusal','lost-response reload replay','reservation display','stock-zero button','no overflow/JS error']});await context.close();
 }} finally{await browser.close();}
 const result={passed:true,scope:'real Edge/static pages, fixture APIs',results};await fs.writeFile(path.join(out,'summary.json'),JSON.stringify(result,null,2));console.log(JSON.stringify(result,null,2));
})().catch(e=>{console.error(e);process.exitCode=1;});
