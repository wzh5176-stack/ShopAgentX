"use strict";
(() => {
  const $ = id => document.getElementById(id);
  const el = (tag, cls = "", text) => { const n = document.createElement(tag); n.className = cls; if (text !== undefined) n.textContent = text; return n; };
  const link = (text, href, cls = "") => { const n = el("a", cls, text); n.href = href; return n; };
  const btn = (text, cls, action) => { const n = el("button", cls, text); n.type = "button"; n.addEventListener("click", action); return n; };
  const positive = n => Number.isSafeInteger(n) && n > 0;
  const price = n => typeof n === "string" && /^(0|[1-9][0-9]{0,9})\.[0-9]{2}$/.test(n);
  const validProduct = p => p && positive(p.id) && typeof p.title === "string" && typeof p.brand === "string" && price(p.price);
  const page = document.body.dataset.storePage;
  const params = new URLSearchParams(location.search);
  const state = { user: null, epoch: 0, authLoading: true, authBusy: false, register: false,
    cart: null, cartLoading: false, cartBusy: false, cartSeq: 0, product: null, detailBusy: false,
    detailDirty: false, goodsSeq: 0, detailSeq: 0, sessionSeq: 0, cartError: "" };
  const current = epoch => state.epoch === epoch;
  let toastTimer;
  function toast(text) { clearTimeout(toastTimer); $("store-toast").textContent = text; $("store-toast").hidden = false; toastTimer = setTimeout(() => { $("store-toast").hidden = true; }, 5500); }
  class ApiError extends Error { constructor(message, status = 0) { super(message); this.status = status; } }
  async function api(path, { method = "GET", body, form, timeoutMs = 20000 } = {}) {
    const epoch = state.epoch, controller = new AbortController(), timeout = setTimeout(() => controller.abort(), timeoutMs);
    const headers = { Accept: "application/json", "X-Requested-With": "XMLHttpRequest" };
    try {
      if (method !== "GET") {
        const r = await fetch("/api/csrf", { credentials: "same-origin", cache: "no-store", headers, signal: controller.signal });
        const token = (await r.json())?.data;
        if (!r.ok || typeof token?.token !== "string" || typeof token?.headerName !== "string") throw new ApiError("安全请求准备失败，请刷新后重试。");
        headers[token.headerName] = token.token;
      }
      if (body !== undefined) headers["Content-Type"] = "application/json";
      if (form) headers["Content-Type"] = "application/x-www-form-urlencoded";
      const response = await fetch(path, { method, credentials: "same-origin", cache: "no-store", headers,
        body: form ? new URLSearchParams(form) : body === undefined ? undefined : JSON.stringify(body), signal: controller.signal });
      if (response.status === 204) return null;
      const payload = await response.json().catch(() => null);
      if (!response.ok) {
        if (response.status === 401 && current(epoch) && state.user && path !== "/api/auth/login") {
          clearPrivate(); toast("登录已失效，请重新登录。");
        }
        throw new ApiError(payload?.error?.message || "请求未完成，请稍后再试。", response.status);
      }
      if (!payload?.data || typeof payload.data !== "object") throw new ApiError("服务返回的数据不完整。");
      return payload.data;
    } catch (e) {
      if (e instanceof ApiError) throw e;
      throw new ApiError(e.name === "AbortError" ? "等待超时，请刷新核对结果，避免重复操作。" : "暂时无法连接服务，请稍后重试。");
    } finally { clearTimeout(timeout); }
  }
  function clearPrivate() {
    state.epoch++; state.cartSeq++; state.user = null; state.cart = null;
    state.cartLoading = false; state.cartBusy = false; state.detailBusy = false; state.cartError = "";
    state.detailDirty = false;
    if (page === "product") { $("detail-quantity").value = "1"; $("detail-feedback").textContent = ""; $("detail-feedback").hidden = true; }
    if (page === "cart") renderCart(); updateAccount();
  }
  function updateAccount() {
    $("store-account").textContent = state.user || "登录 / 注册";
    $("store-account").disabled = !!state.user || state.authLoading;
    $("store-account").title = state.user || "登录或注册";
    $("store-logout").hidden = !state.user;
    $("store-cart-count").textContent = state.authLoading || state.user && !state.cart ? "—" : String(state.cart?.totalQuantity || 0);
    updateDetail();
    document.dispatchEvent(new CustomEvent("shopagent-session", { detail: { username: state.user, loading: state.authLoading, epoch: state.epoch } }));
  }
  function showAuth() { if (!$("store-auth").open) $("store-auth").showModal(); }
  $("store-account").addEventListener("click", showAuth);
  $("close-auth").addEventListener("click", () => $("store-auth").close());
  $("store-auth-toggle").addEventListener("click", () => {
    if (state.authBusy) return; state.register = !state.register;
    $("store-auth-submit").textContent = state.register ? "注册并登录" : "登录";
    $("store-auth-toggle").textContent = state.register ? "已有账号？直接登录" : "还没有账号？注册一个";
    $("store-password").minLength = state.register ? 8 : 1;
    $("store-password").autocomplete = state.register ? "new-password" : "current-password";
    $("store-auth-error").hidden = true;
  });
  $("store-auth-form").addEventListener("submit", async e => {
    e.preventDefault(); if (state.authBusy) return;
    const username = $("store-username").value.trim(), password = $("store-password").value;
    const error = $("store-auth-error"); error.hidden = true;
    if (new TextEncoder().encode(password).length > 72) { error.textContent = "密码UTF-8长度不能超过72字节。"; error.hidden = false; return; }
    state.authBusy = true; state.sessionSeq++; $("store-auth-submit").disabled = true; $("store-auth-toggle").disabled = true;
    let registered = false;
    try {
      if (state.register) { await api("/api/auth/register", { method: "POST", body: { username, password } }); registered = true; }
      await api("/api/auth/login", { method: "POST", form: { username, password } });
      $("store-password").value = ""; $("store-auth").close();
      clearPrivate(); state.user = username; state.authLoading = false; updateAccount();
      await loadCart(); toast("登录成功，接着逛吧。");
    } catch (err) { error.textContent = (registered ? "账号已创建，请切换登录后重试。" : "") + err.message; error.hidden = false; }
    finally { state.authBusy = false; $("store-auth-submit").disabled = false; $("store-auth-toggle").disabled = false; }
  });
  $("store-logout").addEventListener("click", async () => {
    state.sessionSeq++;
    $("store-logout").disabled = true;
    try { await api("/api/auth/logout", { method: "POST" }); clearPrivate(); toast("已退出登录。"); }
    catch (e) { toast(e.message); } finally { $("store-logout").disabled = false; }
  });
  async function session() {
    const seq = ++state.sessionSeq; state.authLoading = true; updateAccount();
    if (page === "cart") { $("cart-page-body").replaceChildren(el("p", "status", "正在确认登录状态…")); }
    try {
      const data = await api("/api/auth/me"); if (seq !== state.sessionSeq) return;
      if (typeof data.username !== "string") throw new ApiError("登录信息不完整。");
      if (state.user !== data.username) { clearPrivate(); state.user = data.username; }
      state.authLoading = false; updateAccount(); await loadCart();
    } catch (e) {
      if (seq !== state.sessionSeq) return;
      clearPrivate(); state.authLoading = false;
      if (e.status !== 401) { toast(e.message); if (page === "cart") renderCartError("暂时无法确认登录状态。" + e.message, session); }
      else if (page === "cart") renderCart();
      updateAccount();
    }
  }
  function picture(p, linked = true) {
    const n = linked ? link("", "./product.html?id=" + p.id, "goods-picture") : el("div", "goods-picture");
    if (linked) n.setAttribute("aria-label", "查看" + p.title);
    const image = document.createElement("img"); image.width = 500; image.height = 500;
    image.src = /鞋/.test(p.title) ? "./shoe.svg" : /耳机/.test(p.title) ? "./headphones.svg" : /鼠标/.test(p.title) ? "./mouse.svg" : "./goods.svg";
    image.alt = p.title + "的分类示意图"; image.loading = "lazy";
    const label = el("small", "", "示意图"); n.append(image, label);
    try { const url = new URL(p.imageUrl); if (["https:", "http:"].includes(url.protocol)) {
      const photo = document.createElement("img"); photo.className = "photo"; photo.alt = p.title; photo.width = 500; photo.height = 500; photo.loading = "lazy"; photo.hidden = true; photo.referrerPolicy = "no-referrer";
      photo.addEventListener("load", () => { photo.hidden = false; image.hidden = true; label.hidden = true; }, { once: true });
      photo.addEventListener("error", () => photo.remove(), { once: true }); photo.src = url.href; n.append(photo);
    } } catch { /* A labeled illustration remains when no real image exists. */ }
    return n;
  }
  function productCard(p) {
    const card = el("article", "goods-card"); card.dataset.productId = String(p.id);
    const copy = el("div", "goods-copy"), title = el("h3"); title.append(link(p.title, "./product.html?id=" + p.id));
    const bottom = el("div", "goods-bottom"), open = link("↗", "./product.html?id=" + p.id, "goods-open"); open.setAttribute("aria-label", "打开" + p.title + "详情");
    bottom.append(el("span", "goods-price", "¥" + p.price), open); copy.append(el("p", "goods-brand", p.brand), title, bottom); card.append(picture(p), copy); return card;
  }
  const keyword = (params.get("keyword") || "").trim();
  const pageNumber = Number(params.get("page") || 1);
  function goodsUrl(number = 1) { const query = new URLSearchParams(); if (keyword) query.set("keyword", keyword); if (number > 1) query.set("page", String(number)); return "./products.html" + (query.size ? "?" + query : ""); }
  async function loadGoods() {
    const seq = ++state.goodsSeq, grid = $("goods-grid"), status = $("goods-status");
    grid.setAttribute("aria-busy", "true"); status.textContent = "正在读取商品…"; $("goods-retry").hidden = true;
    try {
      if (keyword.length > 50 || !positive(pageNumber)) throw new ApiError("搜索词最多50个字符，页码需为正整数。", 400);
      const q = new URLSearchParams({ page: String(page === "home" ? 1 : pageNumber), size: String(page === "home" ? 4 : 8) }); if (keyword && page !== "home") q.set("keyword", keyword);
      const data = await api("/api/products?" + q); if (seq !== state.goodsSeq) return;
      if (!Array.isArray(data.items) || !data.items.every(validProduct) || !Number.isSafeInteger(data.total)) throw new ApiError("商品列表不完整。");
      grid.replaceChildren(...data.items.map(productCard)); status.textContent = "";
      if (!data.items.length) {
        const empty = el("div", "empty-goods"); empty.append(el("h2", "", "这次还没找到合适的商品。"), el("p", "", "换个关键词，或者看看当前全部商品。"), link("查看全部商品", "./products.html", "secondary")); grid.append(empty);
      }
      if (page === "products") {
        $("goods-count").textContent = "共 " + data.total + " 件商品";
        $("goods-pagination").hidden = data.totalPages <= 1;
        $("goods-page").textContent = `第 ${data.page} / ${Math.max(1, data.totalPages)} 页`;
        $("goods-prev").href = goodsUrl(data.page - 1); $("goods-prev").setAttribute("aria-disabled", String(data.page <= 1));
        $("goods-next").href = goodsUrl(data.page + 1); $("goods-next").setAttribute("aria-disabled", String(data.page >= data.totalPages));
      }
    } catch (e) { if (seq === state.goodsSeq) { grid.replaceChildren(); status.textContent = e.message; $("goods-retry").hidden = false; if ($("goods-pagination")) $("goods-pagination").hidden = true; } }
    finally { if (seq === state.goodsSeq) grid.setAttribute("aria-busy", "false"); }
  }
  async function loadProduct() {
    const id = Number(params.get("id")), seq = ++state.detailSeq;
    $("product-status").textContent = "正在读取商品详情…"; $("detail-retry").hidden = true;
    $("product-content").hidden = true; $("related-section").hidden = true; state.product = null;
    try {
      if (!positive(id)) throw new ApiError("商品编号无效，请从商品列表重新选择。", 400);
      const p = await api("/api/products/" + id); if (seq !== state.detailSeq) return;
      if (!validProduct(p) || typeof p.description !== "string") throw new ApiError("商品详情不完整。");
      state.product = p; $("product-crumb").textContent = p.title; document.title = p.title + " · ShopAgent 跳糖选物";
      $("detail-brand").textContent = p.brand; $("detail-title").textContent = p.title; $("detail-price").textContent = "¥" + p.price;
      $("detail-description").textContent = p.description || "暂无更多商品介绍。";
      $("detail-visual").replaceChildren(picture(p, false)); $("product-content").hidden = false; $("product-status").textContent = "";
      $("detail-guide").href = "./play.html?" + new URLSearchParams({ question: "帮我看看「" + p.title + "」适不适合日常使用，也想对比同类商品。" });
      updateDetail();
      try {
        const related = await api("/api/products?" + new URLSearchParams({ keyword: p.brand.slice(0, 50), size: "4", page: "1" }));
        if (seq !== state.detailSeq) return;
        const items = Array.isArray(related.items) ? related.items.filter(item => validProduct(item) && item.id !== p.id) : [];
        if (items.length) { $("related-grid").replaceChildren(...items.map(productCard)); $("related-link").href = "./products.html?" + new URLSearchParams({ keyword: p.brand.slice(0, 50) }); $("related-section").hidden = false; }
      } catch { /* Optional related products do not prevent buying the requested product. */ }
    } catch (e) { if (seq === state.detailSeq) { $("product-status").textContent = e.message; $("detail-retry").hidden = [400, 404].includes(e.status); const back = link("返回商品列表 →", "./products.html", "text-link"); $("product-status").append(document.createElement("br"), back); } }
  }
  function updateDetail() {
    if (page !== "product" || !state.product) return;
    const quantity = $("detail-quantity"), existing = state.cart?.items.find(p => p.productId === state.product.id)?.quantity || 0;
    if (!state.detailDirty) quantity.value = String(existing || 1);
    $("detail-cart-hint").textContent = state.user && !state.cart ? "暂未读到购物车。请先刷新购物车核对数量，再保存。" : existing ? `购物车已有 ${existing} 件，保存后将设为上方的最终总数量。` : "保存时，将此商品的购物车数量设为上面的最终总数量。";
    const locked = state.detailBusy || state.authLoading || state.cartLoading || !!state.user && !state.cart;
    $("detail-save").disabled = locked; quantity.disabled = locked;
    $("quantity-minus").disabled = locked || quantity.valueAsNumber <= 1;
    $("quantity-plus").disabled = locked || quantity.valueAsNumber >= 99;
    $("detail-save").textContent = state.detailBusy ? "正在保存…" : state.authLoading ? "正在确认登录…" : state.user ? "放入购物车" : "登录后放入购物车";
  }
  function validCart(data) { return data && Array.isArray(data.items) && Number.isSafeInteger(data.totalQuantity) && price(data.totalAmount) && data.items.every(item => positive(item.productId) && typeof item.title === "string" && Number.isInteger(item.quantity) && item.quantity >= 1 && item.quantity <= 99 && typeof item.available === "boolean" && price(item.subtotal) && (item.available ? typeof item.brand === "string" && price(item.price) : (item.brand === null || typeof item.brand === "string") && (item.price === null || price(item.price)))); }
  async function loadCart() {
    if (!state.user) return; const epoch = state.epoch, seq = ++state.cartSeq; state.cartLoading = true; state.cartError = ""; updateAccount(); lockCartControls();
    try {
      const data = await api("/api/cart"); if (!current(epoch) || seq !== state.cartSeq) return;
      if (!validCart(data)) throw new ApiError("购物车数据不完整。"); state.cart = data;
    } catch (e) { if (current(epoch) && seq === state.cartSeq) { state.cart = null; state.cartError = e.message; } }
    finally { if (current(epoch) && seq === state.cartSeq) { state.cartLoading = false; updateAccount(); if (page === "cart") renderCart(); } }
  }
  function lockCartControls() { if (page === "cart") $("cart-page-body").querySelectorAll("input,button").forEach(n => { n.disabled = true; }); }
  function cartState(title, description, action, label) {
    const n = el("section", "cart-state"), img = document.createElement("img"); img.src = "./goods.svg"; img.alt = ""; img.width = 170; img.height = 120;
    n.append(img, el("h2", "", title), el("p", "", description));
    n.append(typeof action === "function" ? btn(label, "primary", action) : link(label, action, "primary")); return n;
  }
  function renderCartError(message, retry = loadCart) { const n = cartState("暂时无法读取购物车", message, retry, "重新读取"); n.classList.add("cart-state-error"); $("cart-page-body").replaceChildren(n); }
  function renderCart() {
    if (page !== "cart") return;
    if (!state.user) { $("cart-page-body").replaceChildren(cartState("登录，看看你的挑选。", "你的购物车和导购对话只属于你。登录后可继续保存的商品。", showAuth, "登录 / 注册")); return; }
    if (!state.cart) { if (state.cartLoading) $("cart-page-body").replaceChildren(el("p", "status", "正在读取购物车…")); else renderCartError(state.cartError || "没有读到购物车信息。"); return; }
    if (!state.cart.items.length) { $("cart-page-body").replaceChildren(cartState("购物车还是空的。", "遇见喜欢的商品，就把它留在这里。", "./products.html", "去逛逛商品")); return; }
    const layout = el("div", "cart-layout"), list = el("section", "cart-list"); list.setAttribute("aria-label", "购物车商品");
    for (const item of state.cart.items) {
      const row = el("article", "cart-item"); row.dataset.productId = String(item.productId);
      const main = el("div", "cart-item-main"), title = el("h2"); title.append(item.available ? link(item.title, "./product.html?id=" + item.productId) : document.createTextNode(item.title));
      main.append(title, el("p", "", (item.brand || "已不可售商品") + (item.available ? " · ¥" + item.price + " / 件" : " · 暂不可用，可移除")));
      const form = el("form", "cart-edit"), input = document.createElement("input"); form.dataset.available = String(item.available); input.type = "number"; input.min = "1"; input.max = "99"; input.step = "1"; input.required = true; input.value = String(item.quantity); input.id = "cart-quantity-" + item.productId; input.disabled = !item.available;
      const label = el("label", "", "最终总数量"); label.htmlFor = input.id;
      const save = el("button", "secondary", "保存数量"); save.type = "submit"; save.disabled = !item.available;
      const error = el("p", "cart-row-error"); error.setAttribute("role", "status");
      const remove = btn("移除", "remove-item", () => change(true));
      async function change(removing) {
        if (state.cartBusy || state.cartLoading || state.authLoading) return;
        const quantity = input.valueAsNumber;
        if (!removing && !form.reportValidity()) return;
        state.cartBusy = true; lockCartControls(); const epoch = state.epoch; error.textContent = "";
        try {
          await api("/api/cart/items/" + item.productId, { method: removing ? "DELETE" : "PUT", body: removing ? undefined : { quantity } });
          if (!current(epoch)) return; state.cartBusy = false; await loadCart(); if (current(epoch)) toast(removing ? "商品已移除。" : "数量已保存。");
        } catch (e) { if (current(epoch)) { error.textContent = e.message + (!e.status ? " 请刷新购物车核对执行结果。" : ""); } }
        finally { if (current(epoch)) { state.cartBusy = false;
          $("cart-page-body").querySelectorAll(".cart-edit").forEach(f => { f.querySelectorAll("input,button").forEach(n => { n.disabled = f.dataset.available !== "true" && !n.classList.contains("remove-item"); }); });
          const refresh = $("cart-refresh"); if (refresh) refresh.disabled = false;
        } }
      }
      form.addEventListener("submit", e => { e.preventDefault(); change(false); }); form.append(label, input, save, remove); main.append(form);
      row.append(picture({ ...item, id: item.productId }, item.available), main, el("p", "cart-item-total", item.available ? "¥" + item.subtotal : "—"), error); list.append(row);
    }
    const summary = el("aside", "cart-summary"); summary.setAttribute("aria-label", "购物车合计"); summary.append(el("h2", "", "这次的挑选"));
    const qty = el("div", "summary-line"); qty.append(el("span", "", "商品件数"), el("span", "", state.cart.totalQuantity + " 件"));
    const amount = el("div", "summary-line"); amount.append(el("span", "", "商品合计"), el("strong", "cart-amount", "¥" + state.cart.totalAmount));
    summary.append(qty, amount, el("p", "help", "最终金额与可购买数量将在结算时重新核对。"), link("去结算 →", "./checkout.html", "primary"), link("继续选购", "./products.html", "text-link"));
    const refresh = btn("刷新购物车", "secondary cart-refresh", () => { if (!state.cartBusy) loadCart(); }); refresh.id = "cart-refresh";
    layout.append(list, summary); $("cart-page-body").replaceChildren(layout, refresh);
    if (state.cartBusy || state.cartLoading) lockCartControls();
  }
  if (page === "product") {
    const quantity = $("detail-quantity");
    quantity.addEventListener("input", () => { state.detailDirty = true; updateDetail(); });
    for (const [id, step] of [["quantity-minus", -1], ["quantity-plus", 1]]) $(id).addEventListener("click", () => {
      quantity.value = String(Math.min(99, Math.max(1, (Number.isInteger(quantity.valueAsNumber) ? quantity.valueAsNumber : 1) + step))); state.detailDirty = true; updateDetail();
    });
    $("detail-cart-form").addEventListener("submit", async e => {
      e.preventDefault(); if (state.detailBusy || state.authLoading || state.cartLoading || !state.product) return;
      if (!state.user) return showAuth(); if (!state.cart) { toast("请先打开购物车刷新核对数量。"); return; }
      if (!$("detail-cart-form").reportValidity()) return;
      const epoch = state.epoch, target = quantity.valueAsNumber; state.detailBusy = true; updateDetail(); const feedback = $("detail-feedback"); feedback.hidden = true;
      try {
        await api("/api/cart/items/" + state.product.id, { method: "PUT", body: { quantity: target } });
        if (!current(epoch)) return;
        feedback.textContent = "已保存到购物车，最终数量为 " + target + " 件。"; feedback.hidden = false;
        await loadCart(); if (current(epoch)) toast("已保存，可以到购物车查看。");
      } catch (err) { if (current(epoch)) { feedback.textContent = err.message + (!err.status ? " 请打开购物车刷新核对结果。" : ""); feedback.hidden = false; } }
      finally { if (current(epoch)) { state.detailBusy = false; updateDetail(); } }
    });
    $("detail-retry").addEventListener("click", loadProduct); loadProduct();
  }
  if (page === "home" || page === "products") { $("goods-retry").addEventListener("click", loadGoods); loadGoods(); }
  if (page === "products") {
    $("site-keyword").value = keyword.slice(0, 50);
    document.querySelectorAll("[data-filter]").forEach(n => { if (n.dataset.filter === keyword) n.setAttribute("aria-current", "page"); });
    if (keyword) { $("search-summary").hidden = false; $("search-summary").append(document.createTextNode("正在查找：“" + keyword + "”"), link("清除搜索", "./products.html")); }
  }
  if (page === "knowledge") {
    const question = $("knowledge-question"), form = $("knowledge-form"); let busy = false;
    const validText = (value, limit) => typeof value === "string" && value.trim().length > 0 && value.length <= limit;
    function controls() {
      question.disabled = busy; $("knowledge-submit").disabled = busy;
      $("knowledge-submit").textContent = busy ? "正在查找资料…" : "查资料，找答案";
      $("knowledge-question-count").textContent = question.value.length + " / 1000";
      form.setAttribute("aria-busy", String(busy));
      document.querySelectorAll("[data-knowledge-question]").forEach(n => { n.disabled = busy; });
    }
    function validateKnowledge(data) {
      if (!validText(data.answer, 2000) || typeof data.answerable !== "boolean" || !Array.isArray(data.sources) || data.sources.length > 3 || (data.answerable ? !data.sources.length : !!data.sources.length)) throw new ApiError("回答或资料引用不完整，请稍后重新提问。");
      const ids = new Set();
      for (const source of data.sources) {
        if (!source || !validText(source.chunkId, 200) || !validText(source.source, 200) || !validText(source.document, 2000) || !validText(source.section, 200) || !validText(source.content, 5000) || ids.has(source.chunkId)) throw new ApiError("资料引用无法核对，请稍后重新提问。");
        ids.add(source.chunkId);
      }
    }
    function renderKnowledge(data, text) {
      $("knowledge-asked").textContent = text;
      $("knowledge-answer-status").textContent = data.answerable ? "根据检索资料回答" : "资料中没有足够依据";
      $("knowledge-answer").textContent = data.answer;
      $("knowledge-sources").replaceChildren(...data.sources.map((source, index) => {
        const details = el("details", "knowledge-source"); details.open = index === 0;
        details.append(el("summary", "", source.document.split("\n")[0].replace(/^#+\s*/, "") + " · " + source.section));
        details.append(el("p", "help knowledge-source-document", source.document));
        details.append(el("p", "knowledge-source-content", source.content));
        details.append(el("p", "help knowledge-source-meta", "来源：" + source.source + " · 片段：" + source.chunkId)); return details;
      }));
      $("knowledge-source-heading").hidden = !data.sources.length;
      $("knowledge-result").hidden = false;
    }
    question.addEventListener("input", () => { question.setCustomValidity(""); controls(); });
    question.addEventListener("keydown", e => { if (!e.isComposing && (e.ctrlKey || e.metaKey) && e.key === "Enter") { e.preventDefault(); form.requestSubmit(); } });
    document.querySelectorAll("[data-knowledge-question]").forEach(n => n.addEventListener("click", () => { if (!busy) { question.value = n.dataset.knowledgeQuestion; question.setCustomValidity(""); controls(); question.focus(); } }));
    form.addEventListener("submit", async e => {
      e.preventDefault(); if (busy) return; const text = question.value.trim();
      if (!text || text.length > 1000) { question.setCustomValidity("请输入1～1000个字符的问题。"); question.reportValidity(); return; }
      busy = true; controls(); $("knowledge-result").hidden = true; $("knowledge-sources").replaceChildren();
      $("knowledge-feedback").textContent = "正在检索资料并生成回答，请稍候。"; $("knowledge-feedback").classList.remove("error-text");
      try {
        const data = await api("/api/assistant/knowledge", { method: "POST", body: { question: text }, timeoutMs: 35000 });
        validateKnowledge(data); renderKnowledge(data, text); $("knowledge-feedback").textContent = "";
      } catch (err) { $("knowledge-feedback").textContent = err.message + " 问题已保留；确认服务恢复后，再手动提问。"; $("knowledge-feedback").classList.add("error-text"); }
      finally { busy = false; controls(); }
    });
    controls();
  }
  $("site-search").addEventListener("submit", e => { const input = $("site-keyword"); input.value = input.value.trim(); if (!$("site-search").reportValidity()) e.preventDefault(); });
  document.querySelector(`[data-nav="${page === "product" ? "products" : page}"]`)?.setAttribute("aria-current", "page");
  window.addEventListener("pageshow", e => { if (e.persisted) session(); });
  window.addEventListener("focus", () => { if (!state.authBusy && !state.cartBusy && !state.detailBusy) session(); });
  window.ShopAgentStore = { api, showAuth, toast, picture, getSession: () => ({ username: state.user, loading: state.authLoading, epoch: state.epoch }) };
  session();
})();
