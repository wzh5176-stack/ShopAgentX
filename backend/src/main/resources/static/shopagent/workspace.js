"use strict";
(() => {
  const $ = id => document.getElementById(id);
  const node = (tag, cls = "", text) => {
    const el = document.createElement(tag); el.className = cls;
    if (text !== undefined) el.textContent = text;
    return el;
  };
  const button = (text, cls, fn) => {
    const el = node("button", cls, text); el.type = "button";
    el.addEventListener("click", fn); return el;
  };
  const positive = n => Number.isSafeInteger(n) && n > 0;
  const money = n => typeof n === "string" && /^(0|[1-9][0-9]{0,9})\.[0-9]{2}$/.test(n);
  const uuid = n => typeof n === "string" && /^[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}$/.test(n);
  const state = { user: null, epoch: 0, conversation: null, conversations: [], conversationPage: 0,
    conversationPages: 0, messages: [], beforeId: null, hasMore: false, route: 0, busy: false,
    historyLoading: false, restoring: true, uncertain: false, mode: "guide", source: null, actions: new Map(),
    stream: null, stopped: false, cartSequence: 0, detailSequence: 0, catalogSequence: 0,
    catalogPage: 1, catalogPages: 0, keyword: "", register: false, authBusy: false };
  const feed = $("chat-feed"); const question = $("question"); let toastTimer;
  const current = (epoch, conversation) => epoch === state.epoch &&
    (conversation === undefined || conversation === state.conversation?.id);
  const dateText = value => {
    const d = new Date(value); return Number.isNaN(d.getTime()) ? "" :
      new Intl.DateTimeFormat("zh-CN", { month: "numeric", day: "numeric", hour: "2-digit", minute: "2-digit" }).format(d);
  };
  const remember = (id) => { try { const key = "shopagent.active." + state.user;
    if (id) sessionStorage.setItem(key, String(id)); else sessionStorage.removeItem(key);
  } catch { /* Browsing without storage still works. */ } };
  const remembered = () => { try { const id = Number(sessionStorage.getItem("shopagent.active." + state.user));
    return positive(id) ? id : null; } catch { return null; } };

  class ApiError extends Error {
    constructor(message, status = 0, payload = null) { super(message); this.status = status;
      this.code = payload?.error?.code; this.data = payload?.data; }
  }
  function toast(text) { clearTimeout(toastTimer); $("toast").textContent = text; $("toast").hidden = false;
    toastTimer = setTimeout(() => { $("toast").hidden = true; }, 5500); }
  function feedback(text = "") { $("turn-feedback").textContent = text; $("turn-feedback").hidden = !text; }
  function modal(id) { if (!$(id).open) $(id).showModal(); document.body.classList.add("dialog-open"); }
  document.querySelectorAll("[data-close]").forEach(el => el.addEventListener("click", () => $(el.dataset.close).close()));
  document.querySelectorAll("dialog").forEach(el => el.addEventListener("close", () => {
    document.body.classList.toggle("dialog-open", !!document.querySelector("dialog[open]"));
  }));

  async function api(path, { method = "GET", body, form, signal, timeout = 20000 } = {}) {
    const controller = new AbortController();
    const abort = () => controller.abort(); signal?.addEventListener("abort", abort, { once: true });
    if (signal?.aborted) controller.abort();
    const timer = setTimeout(abort, timeout);
    try {
      const headers = { Accept: "application/json", "X-Requested-With": "XMLHttpRequest" };
      if (method !== "GET") {
        const csrfResponse = await fetch("/api/csrf", { credentials: "same-origin", cache: "no-store", headers: { "X-Requested-With": "XMLHttpRequest" }, signal: controller.signal });
        const csrf = (await csrfResponse.json())?.data;
        if (!csrfResponse.ok || !csrf || typeof csrf.headerName !== "string" || typeof csrf.token !== "string") {
          throw new ApiError("无法准备安全请求，请刷新后重试。");
        }
        headers[csrf.headerName] = csrf.token;
      }
      if (body !== undefined) headers["Content-Type"] = "application/json";
      if (form) headers["Content-Type"] = "application/x-www-form-urlencoded";
      const response = await fetch(path, { method, credentials: "same-origin", cache: "no-store", headers,
        body: form ? new URLSearchParams(form) : body === undefined ? undefined : JSON.stringify(body), signal: controller.signal });
      if (response.status === 204) return null;
      let payload;
      try { payload = await response.json(); } catch { throw new ApiError("服务返回内容无法读取。", response.status); }
      if (!response.ok) {
        if (response.status === 401 && state.user && path !== "/api/auth/login") expireSession();
        throw new ApiError(payload?.error?.message || "请求没有完成，请稍后再试。", response.status, payload);
      }
      if (!payload?.data || typeof payload.data !== "object") throw new ApiError("服务返回数据不完整。", response.status);
      return payload.data;
    } catch (e) {
      if (e instanceof ApiError) throw e;
      throw new ApiError(e.name === "AbortError" ? "等待已结束，操作结果请以最新记录为准。" : "暂时无法连接服务，请稍后再试。");
    } finally { clearTimeout(timer); signal?.removeEventListener("abort", abort); }
  }

  function resetPrivate(keepDraft = false) {
    state.epoch++; state.route++; state.stream?.abort(); state.stream = null;
    state.user = null; state.conversation = null; state.conversations = []; state.messages = [];
    state.actions = new Map(); state.source = null; state.mode = "guide"; state.busy = false;
    state.historyLoading = false; state.restoring = false; state.uncertain = false; state.hasMore = false;
    $("conversation-list").replaceChildren(); $("cart-items").replaceChildren(); $("cart-count").textContent = "0";
    $("cart-quantity").textContent = "0 件商品"; $("cart-amount").textContent = "¥0.00";
    $("cart-dialog").close(); $("more-conversations").hidden = true;
    $("conversation-title").textContent = "这次，想发现什么？";
    $("conversation-status").textContent = "先逛商品，或登录开始对话。";
    $("conversations-status").textContent = "登录后，对话只属于你。";
    if (!keepDraft) question.value = "";
    renderFeed(); updateControls(); feedback();
  }
  function expireSession() { remember(null); resetPrivate(); toast("登录已失效，请重新登录。你的已保存记录仍在。"); }
  function updateControls() {
    $("account-button").textContent = state.user || "登录 / 注册";
    $("account-button").disabled = !!state.user;
    $("logout-button").hidden = !state.user;
    $("account-summary").textContent = state.user ? "你好，" + state.user + "。接着聊，慢慢选。" : "登录后保存你的每次挑选。";
    $("ask-submit").disabled = state.busy || state.historyLoading || state.restoring || state.uncertain;
    $("submit-label").textContent = state.busy ? "正在处理…" : state.uncertain ? "先刷新记录" : !state.user ? "登录后开始" : state.mode === "cart" ? "生成待确认操作" : "发送需求";
    question.disabled = state.busy || state.historyLoading || state.restoring;
    $("new-conversation").disabled = state.busy || state.historyLoading || state.restoring;
    $("refresh-history").disabled = !state.conversation || state.busy || state.historyLoading;
    $("mode-guide").disabled = state.busy; $("mode-cart").disabled = state.busy;
    $("mode-guide").setAttribute("aria-pressed", String(state.mode === "guide"));
    $("mode-cart").setAttribute("aria-pressed", String(state.mode === "cart"));
    $("source-reference").hidden = state.mode !== "cart" || !state.source;
    $("source-description").textContent = state.source ? state.source.recommendation.products.map((p, i) => `${i + 1}. ${p.title}`).join("；") : "";
    $("clear-source").disabled = state.busy;
    $("mode-hint").textContent = state.mode === "cart" ? "请说明商品和最终总数量。生成后还需要你确认。" : "聊聊预算和用途，回答会逐段显示。";
    $("stop-generation").hidden = !state.stream || !state.busy;
    $("question-count").textContent = question.value.length + " / 1000";
    document.querySelectorAll(".conversation-item,[data-question],.reference-button,.cart-plan-button").forEach(el => { el.disabled = state.busy || state.historyLoading || state.restoring; });
  }
  function validProduct(p) { return p && positive(p.id) && typeof p.title === "string" && typeof p.brand === "string" && money(p.price); }
  function validMessage(m) { return m && positive(m.id) && ["user", "assistant"].includes(m.role) && typeof m.content === "string" &&
    ["TEXT", "CART_ACTION"].includes(m.messageType) && (m.messageType === "CART_ACTION" ? uuid(m.cartOperationId) && m.role === "assistant" : m.cartOperationId === null); }
  function validPair(pair) { return pair && validMessage(pair.userMessage) && validMessage(pair.assistantMessage) &&
    pair.userMessage.role === "user" && pair.assistantMessage.role === "assistant" && pair.userMessage.id < pair.assistantMessage.id; }
  function mergeMessages(messages) {
    if (!messages.every(validMessage)) throw new ApiError("聊天记录不完整，请刷新记录。");
    const byId = new Map(state.messages.map(m => [m.id, m])); messages.forEach(m => byId.set(m.id, m));
    state.messages = [...byId.values()].sort((a, b) => a.id - b.id);
  }
  function selectSource(message, index) {
    if (state.busy) return;
    state.source = message; state.mode = "cart";
    if (index !== undefined) question.value = `把第${index + 1}款在购物车中的数量设为1`;
    updateControls(); question.focus();
    $("ask").scrollIntoView({ block: "nearest", behavior: "auto" });
  }
  function setMode(mode) {
    if (state.busy) return;
    if (mode === "cart" && !state.source) state.source = [...state.messages].reverse().find(m => m.role === "assistant" && m.recommendation?.products?.length) || null;
    if (mode === "cart" && !state.source) { toast("先让导购推荐商品，再在那份推荐上选择“设置购物车”。"); return; }
    state.mode = mode; updateControls();
  }
  $("mode-guide").addEventListener("click", () => setMode("guide"));
  $("mode-cart").addEventListener("click", () => setMode("cart"));
  $("clear-source").addEventListener("click", () => { state.source = null; state.mode = "guide"; updateControls(); });

  function productVisual(p) {
    const wrap = node("div", "product-visual"); const img = document.createElement("img");
    img.src = /鞋/.test(p.title) ? "./shoe.svg" : /耳机/.test(p.title) ? "./headphones.svg" : /鼠标/.test(p.title) ? "./mouse.svg" : "./goods.svg";
    img.alt = ""; img.loading = "lazy"; const label = node("span", "illustration-label", "示意图"); wrap.append(img, label);
    try { const url = new URL(p.imageUrl); if (["https:", "http:"].includes(url.protocol)) {
      const photo = document.createElement("img"); photo.className = "product-photo"; photo.alt = p.title; photo.loading = "lazy"; photo.referrerPolicy = "no-referrer"; photo.hidden = true;
      photo.addEventListener("load", () => { photo.hidden = false; img.hidden = true; label.hidden = true; }, { once: true });
      photo.addEventListener("error", () => photo.remove(), { once: true }); photo.src = url.href; wrap.append(photo);
    } } catch { /* Retain a labeled local illustration. */ }
    return wrap;
  }
  function productCard(p, source, index) {
    const card = node("article", "product-card"); card.dataset.productId = String(p.id);
    const info = node("div", "product-card-info"); info.append(node("p", "product-brand", p.brand), node("h3", "product-title", p.title));
    const bottom = node("div", "product-card-bottom"); bottom.append(node("span", "product-price", "¥" + p.price), button("查看详情 ↗", "product-detail-button", () => openDetail(p.id)));
    info.append(bottom);
    if (source) info.append(button("设置购物车", "cart-plan-button", () => selectSource(source, index)));
    card.append(productVisual(p), info); return card;
  }
  function renderMessage(m) {
    const article = node("article", "chat-message is-" + m.role); article.dataset.messageId = String(m.id);
    const label = node("p", "message-label", m.role === "user" ? "你" : "ShopAgent");
    label.append(node("time", "", dateText(m.createdAt))); article.append(label, node("p", "message-content", m.content));
    if (m.recommendation) {
      const rec = m.recommendation;
      if (!Array.isArray(rec.products) || !rec.products.every(validProduct)) throw new ApiError("保存的推荐商品不完整。");
      if (rec.keyword || rec.maxPrice) article.append(node("p", "message-conditions", [rec.keyword, rec.maxPrice ? "最高预算 ¥" + rec.maxPrice : null].filter(Boolean).join(" · ")));
      if (rec.products.length) {
        const grid = node("div", "chat-products"); grid.append(...rec.products.map((p, i) => productCard(p, m, i)));
        article.append(grid, button("以这份推荐调整购物车", "small-button reference-button", () => selectSource(m)));
      }
    }
    if (m.messageType === "CART_ACTION") { const host = node("div", "action-card"); host.dataset.operationId = m.cartOperationId; article.append(host); paintAction(host, m.cartOperationId); }
    return article;
  }
  function renderFeed(scroll = false) {
    if (!state.messages.length) {
      const empty = node("div", "chat-empty"); empty.append(node("span", "empty-mark", "↗"), node("h3", "", "好选择，慢慢聊。"), node("p", "", state.user ? "告诉我品类、预算和用途，也可以接着修改条件。" : "登录后保存你的挑选记录，随时回来继续。")); feed.replaceChildren(empty);
    } else {
      const elements = state.messages.map(renderMessage);
      if (state.hasMore) elements.unshift(button("读取更早的消息", "small-button history-more", () => loadHistory(true).catch(e => feedback(e.message))));
      feed.replaceChildren(...elements);
    }
    if (scroll) feed.scrollTop = feed.scrollHeight;
    updateControls(); refreshActions().catch(() => {});
  }
  function renderConversations() {
    $("conversation-list").replaceChildren(...state.conversations.map(c => {
      const el = button("", "conversation-item", () => chooseConversation(c).catch(e => feedback(e.message)));
      el.setAttribute("aria-current", String(c.id === state.conversation?.id)); el.append(node("strong", "", c.title), node("span", "", dateText(c.updatedAt))); return el;
    }));
    $("more-conversations").hidden = state.conversationPage >= state.conversationPages;
    $("conversations-status").textContent = state.conversations.length ? "只显示你的私人对话。" : "还没有对话，从一个问题开始吧。";
    updateControls();
  }
  async function loadConversations(page = 1) {
    if (!state.user) return; const epoch = state.epoch;
    const data = await api(`/api/conversations?page=${page}&size=20`);
    if (!current(epoch)) return;
    if (!Array.isArray(data.items) || !data.items.every(c => positive(c.id) && typeof c.title === "string")) throw new ApiError("会话列表不完整。");
    const map = new Map((page === 1 ? [] : state.conversations).map(c => [c.id, c])); data.items.forEach(c => map.set(c.id, c));
    state.conversations = [...map.values()]; state.conversationPage = data.page; state.conversationPages = data.totalPages; renderConversations();
  }
  async function chooseConversation(c) {
    if (state.busy) return; state.route++; state.conversation = c; state.messages = []; state.actions = new Map();
    state.source = null; state.mode = "guide"; state.hasMore = false; state.uncertain = false;
    $("conversation-title").textContent = c.title; remember(c.id); feedback(); renderConversations(); renderFeed();
    await loadHistory();
  }
  async function loadHistory(older = false) {
    if (!state.user || !state.conversation || state.historyLoading) return;
    const epoch = state.epoch, cv = state.conversation.id, route = state.route;
    const height = feed.scrollHeight, top = feed.scrollTop; state.historyLoading = true; updateControls();
    try {
      const data = await api(`/api/conversations/${cv}/messages?size=20` + (older && state.beforeId ? "&beforeId=" + state.beforeId : ""));
      if (!current(epoch, cv) || route !== state.route) return;
      if (!Array.isArray(data.items) || !data.items.every(validMessage)) throw new ApiError("历史记录不完整。");
      if (!older) state.messages = [];
      mergeMessages(data.items); state.beforeId = data.nextBeforeId; state.hasMore = !!data.hasMore;
      if (state.source && !state.messages.some(m => m.id === state.source.id)) { state.source = null; state.mode = "guide"; }
      renderFeed(!older);
      if (older) feed.scrollTop = top + feed.scrollHeight - height;
      else await refreshActions(true);
      if (current(epoch, cv)) $("conversation-status").textContent = "已读取保存的记录，可以接着聊。";
    } finally { if (current(epoch, cv) && route === state.route) { state.historyLoading = false; updateControls(); } }
  }
  async function ensureConversation(content) {
    if (state.conversation) return state.conversation.id;
    const epoch = state.epoch; const c = await api("/api/conversations", { method: "POST", body: { title: content.slice(0, 40) } });
    if (!current(epoch)) throw new ApiError("登录状态已变化。");
    if (!positive(c.id)) throw new ApiError("会话创建结果无法读取，请刷新对话列表。");
    state.conversation = c; state.route++; state.messages = []; state.actions = new Map(); state.hasMore = false;
    state.conversations = [c, ...state.conversations.filter(old => old.id !== c.id)]; remember(c.id);
    $("conversation-title").textContent = c.title; renderConversations(); return c.id;
  }
  $("new-conversation").addEventListener("click", () => {
    if (!state.user) return modal("auth-dialog"); if (state.busy || state.historyLoading) return;
    state.route++; state.conversation = null; state.messages = []; state.source = null; state.mode = "guide"; state.actions = new Map(); state.hasMore = false; state.uncertain = false;
    remember(null); $("conversation-title").textContent = "新的小心愿"; $("conversation-status").textContent = "发送第一条消息时会保存新对话。";
    feedback(); renderConversations(); renderFeed(); question.focus();
  });
  $("more-conversations").addEventListener("click", async () => { try { await loadConversations(state.conversationPage + 1); } catch (e) { toast(e.message); } });
  $("refresh-history").addEventListener("click", async () => {
    try { await loadHistory(); state.uncertain = false; feedback(); updateControls(); } catch (e) { feedback(e.message); }
  });

  const statuses = { PENDING: "等待你确认", SUCCEEDED: "已完成", CANCELLED: "已取消", EXPIRED: "已过期", CONFLICTED: "数量已变化", REJECTED: "商品暂不可用" };
  function validAction(a) { return a && uuid(a.operationId) && a.conversationId === state.conversation?.id && positive(a.productId) &&
    typeof a.title === "string" && Number.isInteger(a.quantity) && a.quantity >= 1 && a.quantity <= 99 &&
    Number.isInteger(a.baseQuantity) && a.baseQuantity >= 0 && a.status in statuses; }
  function paintAction(host, id) {
    const entry = state.actions.get(id); host.className = "action-card";
    if (!entry?.data) { host.replaceChildren(node("p", "action-help", entry?.error || "正在读取这次操作的最新状态…"));
      if (entry?.error) host.append(button("重新读取状态", "small-button", () => loadAction(id, true))); return; }
    const a = entry.data;
    if (a.status === "SUCCEEDED") host.classList.add("is-success"); else if (a.status !== "PENDING") host.classList.add("is-closed");
    const quantity = node("div", "action-quantity"); quantity.append(node("span", "", "创建时 " + a.baseQuantity + " 件"), node("span", "", "→"), node("strong", "", a.quantity + " 件"));
    const help = a.status === "PENDING" ? "确认后将设置为上面的最终总数量。有效至 " + dateText(a.expiresAt) :
      a.status === "SUCCEEDED" ? "这次设定已完成。当前数量请查看购物车。" : a.status === "CONFLICTED" ? "购物车数量已被修改，请重新生成操作，避免覆盖新数量。" :
      a.status === "EXPIRED" ? "这次操作已过期，需要的话请重新生成。" : a.status === "REJECTED" ? "商品状态已变化，请重新选购。" : "这次操作没有修改购物车。";
    host.replaceChildren(node("span", "action-status", statuses[a.status]), node("h4", "", a.title), quantity, node("p", "action-help", help));
    const controls = node("div", "action-buttons");
    if (a.status === "PENDING") {
      controls.append(button(entry.busy ? "正在处理…" : "确认设为 " + a.quantity + " 件", "primary-button", () => decideAction(id, "confirm")), button("取消这次操作", "small-button", () => decideAction(id, "cancel")));
    }
    controls.append(button("刷新状态", "small-button", () => loadAction(id, true)));
    controls.querySelectorAll("button").forEach(el => { el.disabled = !!entry.busy || !!entry.loading; }); host.append(controls);
    if (entry.error) host.append(node("p", "action-error", entry.error));
  }
  function repaintAction(id) {
    const follow = feed.scrollHeight - feed.scrollTop - feed.clientHeight < 80;
    feed.querySelectorAll("[data-operation-id]").forEach(host => { if (host.dataset.operationId === id) paintAction(host, id); });
    if (follow) feed.scrollTop = feed.scrollHeight;
  }
  async function loadAction(id, force = false) {
    if (!state.user || !state.conversation || !uuid(id)) return;
    const entry = state.actions.get(id) || { seq: 0 }; if (entry.loading || entry.busy || entry.data && !force) return;
    const epoch = state.epoch, cv = state.conversation.id, seq = ++entry.seq;
    entry.loading = true; entry.error = ""; state.actions.set(id, entry); repaintAction(id);
    try { const data = await api(`/api/conversations/${cv}/cart-actions/${id}`);
      if (!current(epoch, cv) || seq !== entry.seq) return;
      if (!validAction(data) || data.operationId !== id) throw new ApiError("操作信息不完整。"); entry.data = data;
    } catch (e) { if (current(epoch, cv) && seq === entry.seq) entry.error = e.message;
    } finally { if (current(epoch, cv) && seq === entry.seq) { entry.loading = false; repaintAction(id); } }
  }
  async function refreshActions(force = false) {
    const ids = [...new Set(state.messages.filter(m => m.messageType === "CART_ACTION").map(m => m.cartOperationId))];
    const epoch = state.epoch, cv = state.conversation?.id; let index = 0;
    await Promise.all(Array.from({ length: Math.min(3, ids.length) }, async () => { while (index < ids.length && current(epoch, cv)) await loadAction(ids[index++], force); }));
  }
  async function decideAction(id, decision) {
    const entry = state.actions.get(id); if (!entry?.data || entry.busy || entry.data.status !== "PENDING") return;
    const epoch = state.epoch, cv = state.conversation.id; entry.seq++; entry.busy = true; entry.error = ""; repaintAction(id);
    try { const data = await api(`/api/conversations/${cv}/cart-actions/${id}/${decision}`, { method: "POST" });
      if (!current(epoch, cv)) return;
      if (!validAction(data) || data.operationId !== id) throw new ApiError("操作结果未能确认，请刷新状态。");
      entry.data = data; await loadCart();
    } catch (e) {
      if (!current(epoch, cv)) return;
      if (e.status === 409 && validAction(e.data) && e.data.operationId === id) entry.data = e.data;
      entry.error = e.message;
      if (!e.status) { entry.busy = false; await loadAction(id, true); entry.error = "未收到执行结果，请核对上面的最新状态。"; }
    } finally { if (current(epoch, cv)) { entry.busy = false; repaintAction(id); } }
  }

  function preview(content) {
    feed.querySelector(".chat-empty")?.remove();
    const user = node("article", "chat-message is-user is-preview"); user.append(node("p", "message-label", "你 · 本次输入"), node("p", "message-content", content));
    const assistant = node("article", "chat-message is-assistant is-preview"); const text = node("p", "message-content", "正在理解需求…");
    assistant.append(node("p", "message-label", "ShopAgent · 正在生成"), text, node("p", "preview-note", "完整回复保存后，会出现在聊天记录里。")); feed.append(user, assistant); feed.scrollTop = feed.scrollHeight;
    return text;
  }
  async function streamGuide(cv, content, onDelta, controller) {
    const csrf = await api("/api/csrf"); const timer = setTimeout(() => controller.abort(), 85000); let reader;
    try {
      const response = await fetch(`/api/conversations/${cv}/messages/stream`, { method: "POST", credentials: "same-origin",
        headers: { "Content-Type": "application/json", Accept: "text/event-stream", "X-Requested-With": "XMLHttpRequest", [csrf.headerName]: csrf.token }, body: JSON.stringify({ content }), signal: controller.signal });
      if (!response.ok) { let p; try { p = await response.json(); } catch { p = null; }
        if (response.status === 401) expireSession(); throw new ApiError(p?.error?.message || "这次回复没有完成。", response.status, p); }
      if (!response.headers.get("Content-Type")?.includes("text/event-stream") || !response.body) throw new ApiError("没有收到有效的逐段回复。");
      reader = response.body.getReader(); const decoder = new TextDecoder("utf-8", { fatal: true }); let pending = "", answer = "", done = null;
      function event(frame) {
        const lines = frame.split("\n"), kind = lines.find(line => line.startsWith("event:"))?.slice(6).trim();
        const data = lines.filter(line => line.startsWith("data:")).map(line => line.slice(5).trimStart()).join("\n");
        if (!kind || !data) return; const value = JSON.parse(data);
        if (done) throw new ApiError("回复结束后收到意外内容，请刷新记录。");
        if (kind === "error") throw new ApiError(value.message || "回复没有完成，请刷新记录。", value.code === "CONVERSATION_CHANGED" ? 409 : 503, { error: value });
        if (kind === "delta") { if (typeof value.content !== "string") throw new ApiError("文字片段不完整。"); answer += value.content;
          if (answer.length > 10000) throw new ApiError("回复过长，请刷新记录。"); onDelta(answer); }
        else if (kind === "done") { if (!validPair(value)) throw new ApiError("保存结果不完整，请刷新记录。"); done = value; }
        else throw new ApiError("收到未知回复类型，请刷新记录。");
      }
      while (true) { const part = await reader.read(); pending += decoder.decode(part.value || new Uint8Array(), { stream: !part.done });
        pending = pending.replace(/\r\n/g, "\n"); let split;
        while ((split = pending.indexOf("\n\n")) !== -1) { event(pending.slice(0, split)); pending = pending.slice(split + 2); }
        if (pending.length > 100000) throw new ApiError("回复格式异常，请刷新记录。"); if (part.done) break; }
      if (!done) throw new ApiError("连接提前结束，保存结果请以最新记录为准。"); return done;
    } finally { clearTimeout(timer); await reader?.cancel().catch(() => {}); }
  }
  $("ask-form").addEventListener("submit", async e => {
    e.preventDefault(); if (state.busy || state.historyLoading || state.restoring || state.uncertain) return;
    const content = question.value.trim(); if (!content || content.length > 1000) { question.setCustomValidity("请输入1～1000个字符的需求。"); question.reportValidity(); return; }
    if (!state.user) return modal("auth-dialog");
    if (state.mode === "cart" && !state.source) { toast("请先选择一份商品推荐。"); return; }
    const epoch = state.epoch, mode = state.mode; state.busy = true; state.stopped = false; feedback(); updateControls();
    try {
      const cv = await ensureConversation(content); if (!current(epoch, cv)) return;
      $("conversation-status").textContent = mode === "cart" ? "正在识别操作，尚未修改购物车…" : "正在为你挑选…";
      let pair;
      if (mode === "guide") {
        state.stream = new AbortController(); updateControls(); const target = preview(content);
        pair = await streamGuide(cv, content, answer => { if (current(epoch, cv)) { target.textContent = answer; if (feed.scrollHeight - feed.scrollTop - feed.clientHeight < 180) feed.scrollTop = feed.scrollHeight; } }, state.stream);
      } else {
        const data = await api(`/api/conversations/${cv}/turns`, { method: "POST", body: { content, sourceMessageId: state.source.id }, timeout: 85000 });
        if (!current(epoch, cv)) return;
        if (data.type === "guide") pair = data.guide;
        else if (["clarify", "cart_action"].includes(data.type)) { pair = data;
          if (data.type === "cart_action") { if (!validAction(data.cartAction) || pair.assistantMessage?.cartOperationId !== data.cartAction.operationId) throw new ApiError("待确认操作不完整，请刷新记录。");
            state.actions.set(data.cartAction.operationId, { seq: 0, data: data.cartAction }); }
        } else throw new ApiError("回复类型无法读取，请刷新记录。");
      }
      if (!current(epoch, cv)) return;
      if (!validPair(pair)) throw new ApiError("保存后的消息不完整，请刷新记录。");
      mergeMessages([pair.userMessage, pair.assistantMessage]);
      if (pair.assistantMessage.recommendation?.products?.length) state.source = pair.assistantMessage;
      question.value = ""; renderFeed(true); $("conversation-status").textContent = "这次对话已保存。";
      loadConversations().catch(() => {});
    } catch (e) {
      if (!current(epoch)) return;
      const stopped = state.stopped; let message = stopped ? "已停止显示回复。已保存的结果以聊天记录为准。" : e instanceof ApiError ? e.message : "连接中断，请刷新记录确认结果。";
      if (!stopped && (!e.status || e.status >= 500) && !(e.code === "AI_SERVICE_UNAVAILABLE")) state.uncertain = true;
      try { await loadHistory(); } catch { /* Keep the original error and draft. */ }
      if (!current(epoch)) return;
      renderFeed(true); feedback(message + (state.uncertain ? " 请先刷新记录核对结果，避免重复发送。" : ""));
      $("conversation-status").textContent = "输入已保留，请查看上方提示。";
    } finally { if (current(epoch)) { state.busy = false; state.stream = null; updateControls(); } }
  });
  $("stop-generation").addEventListener("click", () => { state.stopped = true; state.stream?.abort(); });
  question.addEventListener("input", () => { question.setCustomValidity(""); updateControls(); });
  question.addEventListener("keydown", e => { if (!e.isComposing && (e.ctrlKey || e.metaKey) && e.key === "Enter") { e.preventDefault(); $("ask-form").requestSubmit(); } });
  document.querySelectorAll("[data-question]").forEach(el => el.addEventListener("click", () => { state.mode = "guide"; question.value = el.dataset.question; updateControls(); question.focus(); }));

  async function enterUser(username) {
    resetPrivate(true); state.user = username; state.restoring = true; const epoch = state.epoch; updateControls();
    try { await loadConversations(); if (!current(epoch)) return;
      const saved = remembered(); let c = state.conversations.find(c => c.id === saved) || state.conversations[0];
      if (saved && !state.conversations.some(c => c.id === saved)) { try { c = await api("/api/conversations/" + saved); } catch (e) { if (e.status !== 404) throw e; remember(null); } }
      if (c && current(epoch)) await chooseConversation(c);
      if (current(epoch)) await loadCart();
    } catch (e) { if (current(epoch)) feedback(e.message); }
    finally { if (current(epoch)) { state.restoring = false; updateControls(); } }
  }
  $("account-button").addEventListener("click", () => modal("auth-dialog"));
  $("auth-toggle").addEventListener("click", () => {
    if (state.authBusy) return; state.register = !state.register;
    $("auth-submit").textContent = state.register ? "注册并登录" : "登录";
    $("auth-toggle").textContent = state.register ? "已有账号？直接登录" : "还没有账号？注册一个";
    $("password").autocomplete = state.register ? "new-password" : "current-password";
    $("password").minLength = state.register ? 8 : 1; $("auth-error").hidden = true;
  });
  $("auth-form").addEventListener("submit", async e => {
    e.preventDefault(); if (state.authBusy) return; const username = $("username").value.trim(), password = $("password").value;
    if (new TextEncoder().encode(password).length > 72 || state.register && password.length < 8) { $("auth-error").textContent = "注册密码至少8个字符，且UTF-8长度不超过72字节。"; $("auth-error").hidden = false; return; }
    state.authBusy = true; $("auth-submit").disabled = true; $("auth-toggle").disabled = true; $("auth-error").hidden = true;
    let registered = false;
    try {
      if (state.register) { await api("/api/auth/register", { method: "POST", body: { username, password } }); registered = true; }
      await api("/api/auth/login", { method: "POST", form: { username, password } });
      $("password").value = ""; $("auth-dialog").close(); await enterUser(username); toast("欢迎回来，接着挑选吧。");
    } catch (e) { $("auth-error").textContent = (registered ? "账号已创建，登录未完成。请切换登录后重试。" : "") + e.message; $("auth-error").hidden = false;
    } finally { state.authBusy = false; $("auth-submit").disabled = false; $("auth-toggle").disabled = false; }
  });
  $("logout-button").addEventListener("click", async () => {
    $("logout-button").disabled = true;
    try { state.stream?.abort(); await api("/api/auth/logout", { method: "POST" }); remember(null); resetPrivate(); toast("已退出，私人记录已从页面清除。");
    } catch (e) { toast(e.message); } finally { $("logout-button").disabled = false; }
  });

  async function loadCart() {
    if (!state.user) return; const epoch = state.epoch, sequence = ++state.cartSequence;
    $("cart-status").textContent = "正在读取最新购物车…";
    $("cart-items").querySelectorAll("button,input").forEach(el => { el.disabled = true; });
    try {
      const data = await api("/api/cart"); if (!current(epoch) || sequence !== state.cartSequence) return;
      if (!Array.isArray(data.items) || !money(data.totalAmount) || !Number.isSafeInteger(data.totalQuantity)) throw new ApiError("购物车数据不完整。");
      $("cart-count").textContent = String(data.totalQuantity); $("cart-quantity").textContent = data.totalQuantity + " 件商品"; $("cart-amount").textContent = "¥" + data.totalAmount;
      $("cart-status").textContent = data.items.length ? "数量是每件商品的最终总数量。" : "购物车还是空的，先和导购聊聊吧。";
      $("cart-items").replaceChildren(...data.items.map(item => {
        if (!positive(item.productId) || !Number.isInteger(item.quantity) || typeof item.title !== "string") throw new ApiError("购物车商品不完整。");
        const row = node("article", "cart-item"); row.dataset.productId = String(item.productId);
        row.append(node("h3", "", item.title), node("p", "", item.available ? "¥" + item.price + " / 件 · 小计 ¥" + item.subtotal : "暂不可用，可从购物车移除"));
        const controls = node("form", "cart-edit"), input = document.createElement("input");
        input.type = "number"; input.min = "1"; input.max = "99"; input.step = "1"; input.required = true; input.value = String(item.quantity); input.id = "quantity-" + item.productId; input.disabled = !item.available;
        const label = node("label", "", "数量"); label.htmlFor = input.id;
        const save = node("button", "small-button", "保存数量"); save.type = "submit"; save.disabled = !item.available;
        const error = node("p", "cart-item-error"); error.setAttribute("role", "status");
        async function change(remove) {
          if (controls.dataset.busy) return;
          const quantity = input.valueAsNumber; if (!remove && (!Number.isInteger(quantity) || quantity < 1 || quantity > 99)) { input.reportValidity(); return; }
          controls.dataset.busy = "true"; controls.querySelectorAll("button,input").forEach(el => { el.disabled = true; }); error.textContent = "";
          try { await api("/api/cart/items/" + item.productId, { method: remove ? "DELETE" : "PUT", body: remove ? undefined : { quantity } });
            if (current(epoch)) { await loadCart(); await refreshActions(true); }
          } catch (e) { if (current(epoch)) { error.textContent = e.message + (!e.status ? " 请刷新购物车核对结果。" : ""); } }
          finally { if (current(epoch)) { delete controls.dataset.busy; input.disabled = !item.available; save.disabled = !item.available; removeButton.disabled = false; } }
        }
        const removeButton = button("移除", "small-button", () => change(true));
        controls.addEventListener("submit", e => { e.preventDefault(); change(false); }); controls.append(label, input, save, removeButton); row.append(controls, error); return row;
      }));
    } catch (e) { if (current(epoch) && sequence === state.cartSequence) { $("cart-status").textContent = e.message; $("cart-items").replaceChildren(); $("cart-quantity").textContent = "暂未读取"; $("cart-amount").textContent = "—"; } }
  }
  $("open-cart").addEventListener("click", () => { if (!state.user) return modal("auth-dialog"); modal("cart-dialog"); loadCart(); });
  $("refresh-cart").addEventListener("click", loadCart);

  async function loadCatalog(page = 1) {
    const sequence = ++state.catalogSequence; $("catalog-grid").setAttribute("aria-busy", "true"); $("catalog-status").textContent = "正在读取商品…"; $("retry-catalog").hidden = true;
    $("previous-page").disabled = true; $("next-page").disabled = true;
    try { const query = new URLSearchParams({ page: String(page), size: "8" }); if (state.keyword) query.set("keyword", state.keyword);
      const data = await api("/api/products?" + query); if (sequence !== state.catalogSequence) return;
      if (!Array.isArray(data.items) || !data.items.every(validProduct)) throw new ApiError("商品列表不完整。");
      $("catalog-grid").replaceChildren(...data.items.map(p => productCard(p))); state.catalogPage = data.page; state.catalogPages = data.totalPages;
      $("catalog-status").textContent = data.items.length ? "" : "暂无匹配商品，换个关键词试试。";
      $("catalog-total").textContent = data.total + " 件"; $("catalog-total").hidden = false; $("pagination").hidden = data.totalPages <= 1;
      $("page-label").textContent = `第 ${data.page} / ${Math.max(1, data.totalPages)} 页`;
    } catch (e) { if (sequence === state.catalogSequence) { $("catalog-status").textContent = e.message; $("catalog-grid").replaceChildren(); $("retry-catalog").hidden = false; $("pagination").hidden = true; } }
    finally { if (sequence === state.catalogSequence) { $("catalog-grid").setAttribute("aria-busy", "false"); $("previous-page").disabled = state.catalogPage <= 1; $("next-page").disabled = state.catalogPage >= state.catalogPages; } }
  }
  function searchCatalog(keyword) { state.keyword = keyword.trim(); $("catalog-keyword").value = state.keyword;
    document.querySelectorAll("[data-keyword]").forEach(el => { const active = el.dataset.keyword === state.keyword; el.classList.toggle("is-active", active); el.setAttribute("aria-pressed", String(active)); }); loadCatalog(1); }
  $("catalog-search-form").addEventListener("submit", e => { e.preventDefault(); searchCatalog($("catalog-keyword").value); });
  document.querySelectorAll("[data-keyword]").forEach(el => el.addEventListener("click", () => searchCatalog(el.dataset.keyword)));
  $("previous-page").addEventListener("click", () => loadCatalog(state.catalogPage - 1)); $("next-page").addEventListener("click", () => loadCatalog(state.catalogPage + 1));
  $("retry-catalog").addEventListener("click", () => loadCatalog(state.catalogPage));
  async function openDetail(id) {
    const sequence = ++state.detailSequence; $("detail-body").replaceChildren(node("p", "dialog-message", "正在读取商品详情…")); modal("product-dialog");
    try { const p = await api("/api/products/" + id); if (sequence !== state.detailSequence || !$("product-dialog").open) return;
      if (!validProduct(p) || typeof p.description !== "string") throw new ApiError("商品详情不完整。");
      const layout = node("div", "detail-layout"), copy = node("div", "detail-copy"), title = node("h2", "", p.title); title.id = "detail-title";
      copy.append(node("p", "product-brand", p.brand), title, node("p", "product-price", "¥" + p.price), node("h3", "", "关于这件商品"), node("p", "detail-description", p.description || "暂无更多商品介绍。")); layout.append(productVisual(p), copy); $("detail-body").replaceChildren(layout);
    } catch (e) { if (sequence === state.detailSequence && $("product-dialog").open) { const title = node("h2", "", "暂时无法查看商品"); title.id = "detail-title"; const box = node("div", "dialog-message"); box.append(title, node("p", "", e.message)); $("detail-body").replaceChildren(box); } }
  }
  $("close-detail").addEventListener("click", () => $("product-dialog").close()); $("product-dialog").addEventListener("close", () => { state.detailSequence++; });
  window.addEventListener("focus", () => { if (state.user && !state.busy) refreshActions(true).catch(() => {}); });
  async function boot() {
    const draft = new URLSearchParams(location.search).get("question");
    if (draft && !question.value) question.value = draft.slice(0, 1000);
    updateControls(); loadCatalog(); const epoch = state.epoch;
    try { const data = await api("/api/auth/me"); if (current(epoch) && typeof data.username === "string") await enterUser(data.username); }
    catch (e) { if (e.status !== 401) toast(e.message); }
    finally { if (current(epoch)) { state.restoring = false; updateControls(); } }
  }
  boot();
})();
