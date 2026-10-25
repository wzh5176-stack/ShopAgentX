(() => {
  "use strict";
  const $ = id => document.getElementById(id), store = window.ShopAgentStore;
  let busy = false, username, pending = null, generation = 0;
  const key = () => "shopagent-restock-" + username;
  function remember() { try { pending ? sessionStorage.setItem(key(), JSON.stringify(pending)) : sessionStorage.removeItem(key()); } catch { /* In-memory retry remains available. */ } }
  function controls() {
    for (const id of ["inventory-product", "inventory-quantity", "inventory-reason"]) $(id).disabled = busy || !!pending;
    $("inventory-submit").disabled = busy;
    $("inventory-refresh").disabled = busy;
    $("inventory-submit").textContent = busy ? "正在核对…" : pending ? "重试并核对上次补货" : "确认补货";
  }
  async function refresh() {
    const id = Number($("inventory-product").value), seq = generation;
    if (!Number.isSafeInteger(id) || id <= 0) return;
    const [stock, log] = await Promise.all([store.api(`/api/admin/products/${id}/inventory`), store.api(`/api/admin/products/${id}/inventory/replenishments`)]);
    if (seq !== generation) return;
    $("inventory-stock").textContent = `可售 ${stock.availableQuantity} 件 · 订单预占 ${stock.reservedQuantity} 件`;
    const rows = log.items.map(row => {
      const article = document.createElement("article"), title = document.createElement("strong"), text = document.createElement("p");
      title.textContent = `补货 +${row.quantity} · ${row.actor} · ${row.createdAt}`;
      text.textContent = `${row.reason}；当次可售 ${row.availableQuantity}，预占 ${row.reservedQuantity}`;
      article.append(title, text); return article;
    });
    $("inventory-history").replaceChildren(...rows);
    if (!rows.length) $("inventory-history").textContent = "暂无补货记录。";
  }
  async function load() {
    const seq = ++generation;
    username = store.getSession().username;
    pending = null; $("inventory-panel").hidden = true;
    if (!username) { $("inventory-status").textContent = "请使用管理员账号登录。普通账号没有补货权限。"; $("inventory-login").hidden = false; return; }
    $("inventory-login").hidden = true;
    try {
      let products = [], current = 1, pages = 1;
      do {
        const data = await store.api(`/api/admin/products?page=${current}&size=50`);
        products.push(...data.items); pages = Math.min(data.totalPages || 1, 20); current++;
      } while (current <= pages);
      if (seq !== generation) return;
      $("inventory-product").replaceChildren(...products.map(p => { const option = document.createElement("option"); option.value = p.id; option.textContent = `#${p.id} ${p.title} · ${p.status === 1 ? "在售" : "已下架"}`; return option; }));
      try { const saved = JSON.parse(sessionStorage.getItem(key())); if (saved?.actor === username && products.some(p => p.id === saved.productId)) pending = saved; } catch { /* Invalid browser state is ignored. */ }
      if (pending) { $("inventory-product").value = String(pending.productId); $("inventory-quantity").value = String(pending.quantity); $("inventory-reason").value = pending.reason; }
      $("inventory-status").textContent = products.length ? "补货会增加可售数量；页面库存是当前快照，实际购买仍在下单时核对。" : "当前没有商品，请先新增商品。";
      $("inventory-panel").hidden = !products.length; controls(); await refresh();
      if (pending) $("inventory-feedback").textContent = "有一次尚未确认的补货，请重试核对同一次请求。";
    } catch (error) { if (seq === generation) $("inventory-status").textContent = error.status === 403 ? "当前账号不是管理员，无法查看或修改库存。请退出后登录管理员账号。" : error.message; }
  }
  $("inventory-login").addEventListener("click", store.showAuth);
  $("inventory-product").addEventListener("change", () => { $("inventory-feedback").textContent = ""; refresh().catch(e => $("inventory-feedback").textContent = e.message); });
  $("inventory-refresh").addEventListener("click", () => refresh().catch(e => $("inventory-feedback").textContent = e.message));
  $("inventory-form").addEventListener("submit", async event => {
    event.preventDefault(); if (busy) return;
    if (!pending) {
      if (!$("inventory-form").reportValidity() || !$("inventory-reason").value.trim()) return;
      const requestKey = Array.from(crypto.getRandomValues(new Uint8Array(16)), n => n.toString(16).padStart(2, "0")).join("");
      pending = {actor:username, productId:Number($("inventory-product").value), requestKey, quantity:Number($("inventory-quantity").value), reason:$("inventory-reason").value.trim()};
      remember();
    }
    busy = true; controls(); const seq = generation;
    try {
      const {productId, requestKey, quantity, reason} = pending;
      const result = await store.api(`/api/admin/products/${productId}/inventory/replenishments`, {method:"POST", body:{requestKey,quantity,reason}});
      if (seq !== generation) return;
      pending = null; remember();
      $("inventory-feedback").textContent = `补货已确认，记录 #${result.id}。重复提交同一请求不会重复入库。`;
      await refresh();
    } catch (error) {
      if (seq !== generation) return;
      if (error.status >= 400 && error.status < 500) { pending = null; remember(); }
      $("inventory-feedback").textContent = error.message + (pending ? " 操作结果尚未确认，请使用重试按钮核对，不要另开一次补货。" : " 请核对输入及补货记录。");
    } finally { busy = false; controls(); }
  });
  document.addEventListener("shopagent-session", event => { if (!event.detail.loading && event.detail.username !== username) load(); });
  if (!store.getSession().loading) load();
})();
