"use strict";
(() => {
  const store = window.ShopAgentStore;
  const $ = id => document.getElementById(id);
  const page = document.body.dataset.storePage, body = $("trade-body"), note = $("trade-message");
  const params = new URLSearchParams(location.search);
  const node = (tag, cls = "", text) => { const n = document.createElement(tag); n.className = cls; if (text !== undefined) n.textContent = text; return n; };
  const link = (text, href, cls = "text-link") => { const n = node("a", cls, text); n.href = href; return n; };
  const positive = value => Number.isSafeInteger(value) && value > 0;
  const money = value => { if (typeof value !== "string" || !/^[0-9]+\.[0-9]{2}$/.test(value)) throw new Error("金额数据不完整，请重新读取。"); return "¥" + value; };
  const time = value => typeof value === "string" ? value.replace("T", " ").slice(0, 19) : "—";
  const labels = { PENDING_PAYMENT: "待付款", PAID: "已支付", CANCELLED: "已取消", EXPIRED: "已超时", PENDING: "待确认", SUCCEEDED: "支付成功", FAILED: "支付失败", CLOSED: "已关闭" };
  let identity, epoch = -1, busy = false, poll, addresses = [], cart, selectedAddress = null, selected = new Set(), preview = null, previewSignature = "", currentOrder = null, payments = [], options = null;
  let listPage = 1, listStatus = "", loadSeq = 0, payUrl = "";
  class Stale extends Error {}
  function message(text = "", error = false) { note.textContent = text; note.dataset.error = String(error); note.setAttribute("role", error ? "alert" : "status"); }
  async function api(path, config) {
    const stamp = store.getSession().epoch;
    const result = await store.api(path, config);
    if (store.getSession().epoch !== stamp || stamp !== epoch) throw new Stale();
    return result;
  }
  function button(text, fn, cls = "secondary", disabled = false) {
    const n = node("button", cls, text); n.type = "button"; n.dataset.tradeAction = "true"; n.dataset.permanent = String(disabled); n.disabled = busy || disabled;
    n.addEventListener("click", fn); return n;
  }
  function paintBusy() {
    body.setAttribute("aria-busy", String(busy));
    document.querySelectorAll("[data-trade-action]").forEach(n => { n.disabled = busy || n.dataset.permanent === "true"; });
    document.querySelectorAll("#trade-body input,#trade-body select,#address-editor input,#address-editor button[type=submit]").forEach(n => {
      if (busy && n.dataset.wasDisabled === undefined) n.dataset.wasDisabled = String(n.disabled);
      if (busy) n.disabled = true;
      else if (n.dataset.wasDisabled !== undefined) { n.disabled = n.dataset.wasDisabled === "true"; delete n.dataset.wasDisabled; }
    });
  }
  async function action(fn) {
    if (busy) return; const stamp = epoch; busy = true; paintBusy(); message();
    try { await fn(); } catch (e) { if (!(e instanceof Stale) && stamp === epoch) message(e.message || "操作暂未完成，请刷新核对结果。", true); }
    finally { if (stamp === epoch) { busy = false; paintBusy(); } }
  }
  function blank(title, text, actionNode) { const n = node("section", "trade-blank"); n.append(node("h2", "", title), node("p", "", text)); if (actionNode) n.append(actionNode); body.replaceChildren(n); }
  function panel(title) { const n = node("section", "trade-panel"); if (title) n.append(node("h2", "", title)); return n; }
  function badge(status) { const n = node("span", "trade-badge", labels[status] || "待核对"); n.dataset.status = status; return n; }
  function addressText(a) { return `${a.province}${a.city}${a.district}\n${a.detailAddress}\n${a.recipientName} · ${a.phone}`; }
  function getDraft(key) { try { const v = JSON.parse(sessionStorage.getItem(key)); return v && typeof v === "object" ? v : null; } catch { return null; } }
  function saveDraft(key, value) { try { sessionStorage.setItem(key, JSON.stringify(value)); } catch { /* Current in-memory key still prevents duplicate clicks. */ } }
  function removeDraft(key) { try { sessionStorage.removeItem(key); } catch {} }
  const draftKey = () => "shopagent:checkout:" + store.getSession().username;
  let draft = null;
  function requestDraft(signature) {
    if (!draft || draft.signature !== signature) {
      const saved = getDraft(draftKey());
      draft = saved?.signature === signature && /^[0-9a-f-]{36}$/.test(saved.requestKey || "") ? saved : { signature, requestKey: crypto.randomUUID() };
      saveDraft(draftKey(), draft);
    }
    return draft;
  }
  async function confirm(title, text) {
    const dialog = $("trade-confirm"); $("trade-confirm-title").textContent = title; $("trade-confirm-text").textContent = text;
    return new Promise(resolve => {
      let answer = false;
      $("trade-confirm-ok").onclick = () => { answer = true; dialog.close(); };
      $("trade-confirm-back").onclick = () => dialog.close();
      dialog.addEventListener("close", () => resolve(answer), { once: true }); dialog.showModal();
    });
  }
  function addressEditor(existing = null) {
    const dialog = $("address-editor"), form = $("address-form"); form.replaceChildren();
    $("address-editor-title").textContent = existing ? "修改收货地址" : "添加收货地址";
    const definitions = [["recipientName", "收货人", 50], ["phone", "联系电话", 20], ["province", "省份", 50], ["city", "城市", 50], ["district", "区县", 50], ["detailAddress", "详细地址", 200]];
    for (const [key, text, max] of definitions) {
      const label = node("label", "", text), input = node("input"); input.name = key; input.id = "address-" + key; input.required = true; input.maxLength = max; input.value = existing?.[key] || "";
      if (key === "phone") { input.type = "tel"; input.pattern = "\\+?[0-9]{6,20}"; input.autocomplete = "tel"; } else input.autocomplete = key === "recipientName" ? "name" : "off";
      label.htmlFor = input.id; label.append(input); form.append(label);
    }
    const error = node("p", "error-text"); error.setAttribute("role", "alert");
    const controls = node("div", "trade-actions"), back = button("暂不保存", () => dialog.close()), submit = node("button", "primary", "保存地址"); submit.type = "submit"; controls.append(back, submit); form.append(error, controls);
    form.onsubmit = e => {
      e.preventDefault(); if (!form.reportValidity() || busy) return;
      const data = Object.fromEntries(new FormData(form));
      action(async () => {
        try {
          const saved = await api("/api/addresses" + (existing ? "/" + existing.id : ""), { method: existing ? "PUT" : "POST", body: data });
          selectedAddress = saved.id; preview = null; dialog.close(); await loadAddresses();
          if (page === "checkout") renderCheckout(); else renderAddresses(); message("地址已保存。");
        } catch (e) { if (!(e instanceof Stale)) error.textContent = e.message; throw e; }
      });
    };
    dialog.showModal();
  }
  async function loadAddresses() {
    const data = await api("/api/addresses"); if (!Array.isArray(data.items)) throw new Error("地址数据不完整。");
    addresses = data.items; if (!addresses.some(a => a.id === selectedAddress)) selectedAddress = (addresses.find(a => a.defaultAddress) || addresses[0])?.id || null;
  }
  function renderAddresses() {
    const wrap = panel("收货地址"), controls = node("div", "trade-actions");
    controls.append(button("添加地址", () => addressEditor(), "primary"), link("继续去结算 →", "./checkout.html")); wrap.append(controls);
    if (!addresses.length) wrap.append(node("p", "trade-note", "还没有收货地址，先添加一个，结算时就能直接选择。"));
    for (const a of addresses) {
      const card = node("article", "trade-card"), head = node("div", "trade-card-head"); head.append(node("h3", "", a.recipientName)); if (a.defaultAddress) head.append(badge("默认地址"));
      if (a.defaultAddress) head.lastChild.textContent = "默认地址";
      const actions = node("div", "trade-actions"); actions.append(button("修改", () => addressEditor(a)));
      if (!a.defaultAddress) actions.append(button("设为默认", () => action(async () => { await api(`/api/addresses/${a.id}/default`, { method: "PUT" }); await loadAddresses(); renderAddresses(); message("默认地址已更新。"); })));
      actions.append(button("删除", () => action(async () => { if (!await confirm("删除这个地址？", "已创建订单中的收货信息不会因此改变。")) return; await api(`/api/addresses/${a.id}`, { method: "DELETE" }); await loadAddresses(); renderAddresses(); message("地址已删除。"); }), "secondary trade-danger"));
      card.append(head, node("p", "trade-address", addressText(a)), actions); wrap.append(card);
    }
    body.replaceChildren(wrap);
  }
  function checkoutRequest() { return { addressId: selectedAddress, items: (cart?.items || []).filter(i => selected.has(i.productId) && i.available).map(i => ({ productId: i.productId, quantity: i.quantity })) }; }
  function invalidatePreview() { preview = null; previewSignature = ""; renderCheckout(); }
  function lineItem(item) {
    const row = node("article", "trade-line"), img = node("img"); img.src = "./goods.svg"; img.alt = "商品示意图"; img.width = 76; img.height = 76;
    if (item.imageUrl) { try { const u = new URL(item.imageUrl, location.href); if (u.protocol === "https:" || u.origin === location.origin) { img.src = u.href; img.alt = item.title; img.onerror = () => { img.onerror = null; img.src = "./goods.svg"; img.alt = "商品示意图"; }; } } catch {} }
    const description = node("div"); description.append(node("h3", "", item.title), node("p", "", `${money(item.unitPrice)} × ${item.quantity}`));
    row.append(img, description, node("strong", "", money(item.lineAmount))); return row;
  }
  function renderCheckout() {
    if (!cart?.items?.length) return blank("购物车还是空的。", "先挑选商品，再来确认地址和订单。", link("去选商品", "./products.html", "primary"));
    const grid = node("div", "trade-grid"), main = node("div"), addressPanel = panel("1. 送到哪里"), list = node("div", "trade-list");
    for (const a of addresses) {
      const label = node("label", "trade-choice"), input = node("input"); input.type = "radio"; input.name = "checkout-address"; input.value = String(a.id); input.checked = a.id === selectedAddress;
      input.addEventListener("change", () => { selectedAddress = a.id; invalidatePreview(); }); const text = node("span"); text.append(node("strong", "", a.recipientName + (a.defaultAddress ? " · 默认" : "")), node("small", "trade-address", addressText(a))); label.append(input, text); list.append(label);
    }
    addressPanel.append(list); const addressActions = node("div", "trade-actions"); addressActions.style.marginTop = "18px";
    addressActions.append(button("添加地址", () => addressEditor()), button("刷新地址", () => action(async () => { await loadAddresses(); invalidatePreview(); })), link("管理地址", "./addresses.html")); addressPanel.append(addressActions);
    if (!addresses.length) addressPanel.append(node("p", "trade-note", "请先添加收货地址。"));
    const items = panel("2. 这次买什么"), choices = node("div", "trade-list");
    for (const i of cart.items) {
      const label = node("label", "trade-choice"), input = node("input"); input.type = "checkbox"; input.checked = selected.has(i.productId); input.disabled = !i.available; input.value = String(i.productId);
      input.addEventListener("change", () => { if (input.checked) selected.add(i.productId); else selected.delete(i.productId); invalidatePreview(); });
      const text = node("span"); text.append(node("strong", "", i.title || "暂不可用商品"), node("small", "", i.available ? `${i.quantity} 件 · ${money(i.price)} / 件` : "当前不可购买，未计入本次订单")); label.append(input, text); choices.append(label);
    }
    items.append(choices, node("p", "trade-note", "需要改数量？返回购物车修改后重新核对。下单后购物车商品仍保留。"));
    const summary = panel("3. 核对与提交"); summary.classList.add("trade-summary");
    const request = checkoutRequest(), signature = JSON.stringify(request), count = request.items.length;
    summary.append(node("p", "trade-small", `已选 ${count} 种商品`));
    if (preview && previewSignature === signature) {
      summary.append(node("p", "trade-total", money(preview.totalAmount)), node("p", "trade-note", "以上是服务端核对后的金额，实际成交以订单为准。"));
      const saved = requestDraft(signature);
      if (positive(saved.orderId)) {
        summary.append(link("查看已提交订单", "./order.html?id=" + saved.orderId, "primary"), button("开始一笔新订单", () => { draft = null; removeDraft(draftKey()); preview = null; renderCheckout(); }));
      } else summary.append(button("提交订单", () => action(async () => {
        const token = requestDraft(signature);
        const created = await api("/api/orders", { method: "POST", body: { ...request, requestKey: token.requestKey }, timeoutMs: 35000 });
        if (!positive(created.id)) throw new Error("没有读到订单编号，请保留当前页面并使用原请求重试。");
        token.orderId = created.id; saveDraft(draftKey(), token); location.assign("./order.html?id=" + created.id);
      }), "primary"));
      const reviewed = panel("已核对的商品"), rows = node("div", "trade-lines"); preview.items.forEach(i => rows.append(lineItem(i))); reviewed.append(rows); main.append(reviewed);
    } else {
      summary.append(node("p", "trade-total", "待核对"), node("p", "trade-note", "选择地址和商品后，先核对当前价格，再提交订单。"));
    }
    const invalid = !positive(selectedAddress) || count < 1 || count > 20;
    summary.append(button(preview ? "重新核对金额" : "核对订单金额", () => action(async () => {
      preview = await api("/api/checkout/preview", { method: "POST", body: request }); previewSignature = signature;
      if (!Array.isArray(preview.items)) throw new Error("核价结果不完整。"); renderCheckout(); message("金额已核对，请确认后提交订单。");
    }), "secondary", invalid));
    if (count > 20) summary.append(node("p", "trade-invalid", "每笔订单最多选择20种商品。"));
    summary.append(link("返回购物车", "./cart.html", "text-link"), node("p", "trade-checkout-check", "请求超时时，保留当前选择并使用原请求重试，避免重复下单。"));
    main.prepend(addressPanel, items); grid.append(main, summary); body.replaceChildren(grid);
  }
  async function loadOrders() {
    const q = new URLSearchParams({ page: String(listPage), size: "10" }); if (listStatus) q.set("status", listStatus);
    const data = await api("/api/orders?" + q); if (!Array.isArray(data.items)) throw new Error("订单列表不完整。");
    const section = panel("我的订单"), filter = node("div", "trade-filter"), label = node("label", "", "订单状态"), select = node("select"); select.id = "order-filter"; label.htmlFor = select.id;
    for (const [value, text] of [["", "全部订单"], ["PENDING_PAYMENT", "待付款"], ["PAID", "已支付"], ["CANCELLED", "已取消"], ["EXPIRED", "已超时"]]) { const option = node("option", "", text); option.value = value; select.append(option); }
    select.value = listStatus; select.onchange = () => action(async () => { listStatus = select.value; listPage = 1; await loadOrders(); }); filter.append(label, select, button("刷新列表", () => action(loadOrders)), link("继续选购", "./products.html")); section.append(filter);
    if (!data.items.length) section.append(node("p", "trade-note", "这里还没有符合条件的订单。"));
    for (const o of data.items) {
      if (!positive(o.id)) throw new Error("订单编号不完整。"); const card = node("article", "trade-card"), row = node("div", "trade-order-row"), info = node("div");
      info.append(node("h3", "", "订单 #" + o.id), node("p", "", "创建于 " + time(o.createdAt))); row.append(info, badge(o.status), node("strong", "", money(o.totalAmount)), link("查看订单 →", "./order.html?id=" + o.id, "secondary")); card.append(row); section.append(card);
    }
    const pagination = node("div", "trade-pagination"); pagination.append(button("上一页", () => action(async () => { listPage--; await loadOrders(); }), "secondary", listPage <= 1), node("span", "", `第 ${data.page} / ${Math.max(1, data.pages)} 页 · 共 ${data.total} 笔`), button("下一页", () => action(async () => { listPage++; await loadOrders(); }), "secondary", listPage >= data.pages)); body.replaceChildren(section, pagination);
  }
  async function loadOrder() {
    const id = Number(params.get("id")); if (!positive(id)) return blank("订单编号不正确", "请从我的订单中选择一笔订单。", link("查看我的订单", "./orders.html", "primary"));
    const seq = ++loadSeq;
    const [order, history, modes] = await Promise.all([api(`/api/orders/${id}`), api(`/api/orders/${id}/payments`), api("/api/payments/options")]);
    if (seq !== loadSeq) return; if (!Array.isArray(order.items) || !Array.isArray(history.items)) throw new Error("订单数据不完整。");
    currentOrder = order; payments = history.items; options = modes; renderOrder();
    clearTimeout(poll); if (order.status === "PENDING_PAYMENT") poll = setTimeout(async () => { if (!busy && !document.hidden) { try { await loadOrder(); } catch {} } }, 5000);
  }
  function renderOrder() {
    const o = currentOrder, grid = node("div", "trade-grid"), main = node("div"), receipt = node("div", "trade-receipt");
    receipt.append(node("strong", "", labels[o.status] || "待核对"), node("p", "", o.status === "PAID" ? "付款结果已由后端确认，订单已入账。" : o.status === "PENDING_PAYMENT" ? "订单已创建。请在截止时间前完成付款。" : "这笔订单已结束，可返回商城重新选购。")); main.append(receipt);
    const info = panel("订单 #" + o.id), facts = node("dl", "trade-facts");
    for (const [name, value] of [["创建时间", time(o.createdAt)], ["付款截止", time(o.expiresAt)], ["支付时间", time(o.paidAt)], ["关闭时间", time(o.closedAt)]]) facts.append(node("dt", "", name), node("dd", "", value));
    info.append(facts); const address = panel("收货信息"); address.append(node("p", "trade-address", addressText(o.address))); const items = panel("购买的商品"), rows = node("div", "trade-lines"); o.items.forEach(i => rows.append(lineItem(i))); items.append(rows); main.append(info, address, items);
    const summary = panel("订单金额"); summary.classList.add("trade-summary"); summary.append(node("p", "trade-total", money(o.totalAmount)), link("返回我的订单", "./orders.html"));
    const active = payments.find(p => p.status === "PENDING"), closing = payments.some(p => p.channel === "ALIPAY_SANDBOX" && p.status === "CLOSED");
    if (closing) payUrl = "";
    if (o.status === "PENDING_PAYMENT") {
      if (!closing && options.alipaySandboxEnabled && (!active || active.channel === "ALIPAY_SANDBOX")) {
        summary.append(button(active ? "继续准备沙箱付款" : "准备沙箱付款", () => action(async () => {
          const storageKey = `shopagent:payment:${store.getSession().username}:${o.id}`;
          let token = getDraft(storageKey); if (!/^[0-9a-f-]{36}$/.test(token?.requestKey || "")) { token = { requestKey: crypto.randomUUID() }; saveDraft(storageKey, token); }
          const data = active ? await api(`/api/payments/alipay/${active.id}/page`, { method: "POST" }) : await api("/api/payments/alipay/page", { method: "POST", body: { orderId: o.id, requestKey: token.requestKey } });
          const url = new URL(data.payUrl); if (url.origin !== "https://openapi-sandbox.dl.alipaydev.com" || url.username || url.password) throw new Error("付款链接不符合沙箱地址要求。");
          payUrl = url.href; await loadOrder(); message("沙箱付款已准备好。打开收银台付款后，返回本页查询结果。");
        }), "primary"));
        if (payUrl) { const open = link("打开沙箱收银台 ↗", payUrl, "trade-sandbox-link"); open.target = "_blank"; open.rel = "noopener noreferrer"; summary.append(open); }
      }
      if (active) summary.append(button("查询付款结果", () => action(async () => { await api(`/api/payments/${active.id}/refresh`, { method: "POST", timeoutMs: 35000 }); await loadOrder(); message("已查询付款结果。"); })));
      if (!closing && options.mockEnabled && (!active || active.channel === "MOCK")) {
        if (!active) summary.append(button("创建本地演练支付", () => action(async () => {
          const key = `shopagent:mock:${store.getSession().username}:${o.id}:${payments[0]?.id || "initial"}`;
          let token = getDraft(key); if (!/^[0-9a-f-]{36}$/.test(token?.requestKey || "")) { token = { requestKey: crypto.randomUUID() }; saveDraft(key, token); }
          await api("/api/payments", { method: "POST", body: { orderId: o.id, requestKey: token.requestKey } }); await loadOrder();
        })));
        else {
          for (const [value, text] of [["SUCCEEDED", "模拟付款成功"], ["FAILED", "模拟付款失败"]]) summary.append(button(text, () => action(async () => { await api(`/api/payments/${active.id}/mock-result`, { method: "POST", body: { outcome: value } }); await loadOrder(); message("演练结果已更新。"); })));
        }
      }
      if (!options.mockEnabled && !options.alipaySandboxEnabled) summary.append(node("p", "trade-note", "付款方式暂未开启，请稍后再试。"));
      if (closing) summary.append(node("p", "trade-note", "支付已关闭，订单正在收尾。可再次取消以同步订单状态。"));
      summary.append(button("取消订单", () => action(async () => { if (!await confirm("确认取消这笔订单？", "已付款的订单无法直接取消。结果暂不明确时，将保留库存并提示你重试。")) return; try { await api(`/api/orders/${o.id}/cancel`, { method: "POST", timeoutMs: 45000 }); payUrl = ""; await loadOrder(); message("订单状态已更新。"); } catch (e) { await loadOrder().catch(() => {}); throw e; } }), "secondary trade-danger"));
      summary.append(node("p", "trade-note", "本项目使用测试支付，不涉及真实资金。支付状态以本页从后端读取的结果为准。"));
    } else { payUrl = ""; summary.append(link("继续逛逛", "./products.html", "primary")); }
    if (payments.length) { const history = panel("付款记录"), records = node("div", "trade-list"); for (const p of payments) { const row = node("div", "trade-card-head"); row.append(node("span", "", `${p.channel === "MOCK" ? "本地演练" : "支付宝沙箱"} · ${money(p.amount)}`), badge(p.status)); records.append(row); } history.append(records, node("p", "trade-note", "显示最近20次付款记录。")); main.append(history); }
    grid.append(main, summary); body.replaceChildren(grid); paintBusy();
  }
  async function loadPage() {
    const seq = ++loadSeq; body.replaceChildren(node("p", "trade-progress", "正在读取你的信息…"));
    try {
      if (page === "addresses") { await loadAddresses(); if (seq !== loadSeq) return; renderAddresses(); }
      else if (page === "checkout") {
        const [, data] = await Promise.all([loadAddresses(), api("/api/cart")]); if (seq !== loadSeq) return;
        if (!Array.isArray(data.items)) throw new Error("购物车数据不完整。"); cart = data; selected = new Set(data.items.filter(i => i.available).map(i => i.productId)); renderCheckout();
      } else if (page === "orders") await loadOrders();
      else if (page === "order") await loadOrder();
    } catch (e) { if (!(e instanceof Stale)) { message(e.message, true); blank("暂时没有读取成功", "你的数据仍然保留，请稍后重新读取。", button("重新读取", loadPage, "primary")); } }
  }
  function sessionChanged() {
    const session = store.getSession(); if (session.loading) return;
    const next = `${session.epoch}:${session.username || ""}`; if (next === identity) return;
    identity = next; epoch = session.epoch; ++loadSeq; clearTimeout(poll); busy = false; addresses = []; cart = null; preview = null; draft = null; currentOrder = null; payments = []; payUrl = ""; message();
    if ($("address-editor").open) $("address-editor").close(); if ($("trade-confirm").open) $("trade-confirm").close(); $("address-form").replaceChildren();
    if (!session.username) blank("登录，继续这次挑选。", "收货地址、订单与付款记录仅对你本人可见。", button("登录 / 注册", store.showAuth, "primary"));
    else loadPage();
  }
  $("address-editor-close").addEventListener("click", () => { if (!busy) $("address-editor").close(); });
  document.addEventListener("shopagent-session", sessionChanged);
  document.addEventListener("visibilitychange", () => { if (!document.hidden && page === "order" && store.getSession().username && !busy) loadOrder().catch(e => { if (!(e instanceof Stale)) message(e.message, true); }); });
  sessionChanged();
})();
