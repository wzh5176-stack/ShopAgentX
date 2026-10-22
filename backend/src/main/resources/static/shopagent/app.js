"use strict";

(() => {
  const $ = (id) => document.getElementById(id);
  const state = { asking: false, catalogPage: 1, catalogPages: 0, keyword: "", catalogSequence: 0, detailSequence: 0 };
  const askForm = $("ask-form");
  const question = $("question");
  const catalogGrid = $("catalog-grid");
  const dialog = $("product-dialog");

  class ApiError extends Error {
    constructor(message, status = 0) {
      super(message);
      this.status = status;
    }
  }

  async function fetchData(path, { method = "GET", body, headers = {}, timeout = 12000 } = {}) {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeout);
    try {
      const response = await fetch(path, {
        method,
        credentials: "same-origin",
        headers: { Accept: "application/json", ...(body === undefined ? {} : { "Content-Type": "application/json" }), ...headers },
        body: body === undefined ? undefined : JSON.stringify(body),
        signal: controller.signal
      });
      let payload;
      try {
        payload = await response.json();
      } catch {
        throw new ApiError("服务返回了无法读取的内容，请稍后再试。", response.status);
      }
      if (!response.ok) {
        const fallback = response.status === 503 ? "服务暂时不可用，请稍后再试。" :
          response.status === 403 ? "页面安全状态已更新，请重新提交。" : "这次请求没有成功，请稍后再试。";
        throw new ApiError(typeof payload?.error?.message === "string" ? payload.error.message : fallback, response.status);
      }
      if (payload === null || typeof payload !== "object" || payload.data === null || typeof payload.data !== "object") {
        throw new ApiError("服务返回的数据不完整，请稍后再试。");
      }
      return payload.data;
    } catch (error) {
      if (error instanceof ApiError) throw error;
      if (error.name === "AbortError") throw new ApiError("等待时间较长，请稍后重新提交。");
      throw new ApiError("暂时无法连接服务，请确认服务已启动后重试。");
    } finally {
      clearTimeout(timer);
    }
  }

  function textElement(tag, className, text) {
    const element = document.createElement(tag);
    if (className) element.className = className;
    if (text !== undefined) element.textContent = text;
    return element;
  }

  function validProduct(product) {
    return product && Number.isSafeInteger(product.id) && product.id > 0 &&
      typeof product.title === "string" && typeof product.brand === "string" &&
      typeof product.price === "string" && /^(0|[1-9][0-9]{0,7})\.[0-9]{2}$/.test(product.price);
  }

  function illustrationFor(title) {
    if (/鞋/.test(title)) return "./shoe.svg";
    if (/耳机/.test(title)) return "./headphones.svg";
    if (/鼠标/.test(title)) return "./mouse.svg";
    return "./goods.svg";
  }

  function productVisual(product) {
    const visual = textElement("div", "product-visual");
    const illustration = document.createElement("img");
    illustration.src = illustrationFor(product.title);
    illustration.alt = "";
    illustration.loading = "lazy";
    const label = textElement("span", "illustration-label", "示意图");
    visual.append(illustration, label);

    if (typeof product.imageUrl === "string" && product.imageUrl.trim()) {
      try {
        const url = new URL(product.imageUrl);
        if (url.protocol === "https:" || url.protocol === "http:") {
          const photo = document.createElement("img");
          photo.className = "product-photo";
          photo.alt = product.title + "的商品图片";
          photo.loading = "lazy";
          photo.hidden = true;
          photo.addEventListener("load", () => {
            photo.hidden = false;
            illustration.hidden = true;
            label.hidden = true;
          }, { once: true });
          photo.addEventListener("error", () => photo.remove(), { once: true });
          photo.src = url.href;
          visual.append(photo);
        }
      } catch {
        // Keep the clearly labeled local illustration when the URL is invalid.
      }
    }
    return visual;
  }

  function priceElement(value) {
    const price = textElement("span", "product-price");
    const amount = value.replace(/\B(?=(\d{3})+(?!\d))/g, ",");
    price.append(textElement("span", "currency", "¥"), document.createTextNode(amount));
    return price;
  }

  function productCard(product) {
    const card = textElement("article", "product-card");
    card.dataset.productId = String(product.id);
    const info = textElement("div", "product-card-info");
    info.append(textElement("p", "product-brand", product.brand), textElement("h3", "product-title", product.title));
    const bottom = textElement("div", "product-card-bottom");
    const button = textElement("button", "product-detail-button", "查看详情 ↗");
    button.type = "button";
    button.setAttribute("aria-label", "查看" + product.title + "的详情");
    button.addEventListener("click", () => openDetail(product.id));
    bottom.append(priceElement(product.price), button);
    info.append(bottom);
    card.append(productVisual(product), info);
    return card;
  }

  function renderCards(target, products) {
    if (!Array.isArray(products) || !products.every(validProduct)) {
      throw new ApiError("商品数据不完整，请稍后再试。");
    }
    target.replaceChildren(...products.map(productCard));
  }

  function friendlyError(error) {
    return error instanceof ApiError ? error.message : "这次操作没有完成，请稍后再试。";
  }

  function updateCount() {
    $("question-count").textContent = question.value.length + " / 1000";
    question.setCustomValidity("");
  }

  question.addEventListener("input", updateCount);
  question.addEventListener("keydown", (event) => {
    if ((event.ctrlKey || event.metaKey) && event.key === "Enter") {
      event.preventDefault();
      if (!state.asking) askForm.requestSubmit();
    }
  });

  document.querySelectorAll("[data-question]").forEach((button) => {
    button.addEventListener("click", () => {
      question.value = button.dataset.question;
      updateCount();
      question.focus();
    });
  });

  function setAsking(loading) {
    state.asking = loading;
    $("ask").classList.toggle("is-loading", loading);
    $("ask-submit").disabled = loading;
    question.disabled = loading;
    document.querySelectorAll("[data-question]").forEach((button) => { button.disabled = loading; });
    $("submit-label").textContent = loading ? "正在为你挑选…" : "帮我选一选";
    $("recommendations").setAttribute("aria-busy", String(loading));
  }

  function showConditions(keyword, maxPrice) {
    const tags = [
      "关键词：" + (keyword || "待补充"),
      maxPrice === null ? "未设预算上限" : "最高预算 ¥" + maxPrice
    ];
    $("conditions").replaceChildren(...tags.map((text) => textElement("span", "condition-tag", text)));
  }

  askForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (state.asking) return;
    const value = question.value.trim();
    if (!value) {
      question.setCustomValidity("先告诉我你想买什么商品。");
      question.reportValidity();
      return;
    }
    if (value.length > 1000) return;
    setAsking(true);
    $("recommendations").hidden = false;
    $("recommendation-grid").replaceChildren();
    $("conditions").replaceChildren();
    $("recommendation-retry").hidden = true;
    $("answer-panel").classList.remove("is-error");
    $("answer-panel").classList.add("is-loading");
    $("answer-label").textContent = "正在为你挑选";
    $("answer-text").textContent = "正在理解需求并寻找合适商品，请稍候。";
    try {
      const csrf = await fetchData("/api/csrf");
      if (typeof csrf.headerName !== "string" || typeof csrf.token !== "string" || !csrf.token) {
        throw new ApiError("暂时无法准备请求，请重新提交。");
      }
      const result = await fetchData("/api/assistant/ask", {
        method: "POST",
        body: { question: value },
        headers: { [csrf.headerName]: csrf.token },
        timeout: 65000
      });
      if (typeof result.answer !== "string" || typeof result.keyword !== "string" ||
        !(result.maxPrice === null || typeof result.maxPrice === "string")) {
        throw new ApiError("推荐结果不完整，请稍后再试。");
      }
      renderCards($("recommendation-grid"), result.products);
      showConditions(result.keyword, result.maxPrice);
      $("answer-label").textContent = result.products.length ? "ShopAgent 的建议" : "关于这次需求";
      $("answer-text").textContent = result.answer;
    } catch (error) {
      $("answer-panel").classList.add("is-error");
      $("answer-label").textContent = "这次没有完成推荐";
      $("answer-text").textContent = friendlyError(error);
      $("recommendation-retry").hidden = false;
    } finally {
      $("answer-panel").classList.remove("is-loading");
      setAsking(false);
    }
    $("recommendations").scrollIntoView({ behavior: matchMedia("(prefers-reduced-motion: reduce)").matches ? "auto" : "smooth", block: "start" });
  });

  $("retry-ask").addEventListener("click", () => askForm.requestSubmit());
  $("edit-question").addEventListener("click", () => {
    question.scrollIntoView({ behavior: "smooth", block: "center" });
    question.focus({ preventScroll: true });
  });

  async function loadCatalog(page = 1) {
    const sequence = ++state.catalogSequence;
    catalogGrid.setAttribute("aria-busy", "true");
    $("catalog-status").textContent = "正在读取商品…";
    $("catalog-status").classList.remove("is-error");
    $("catalog-retry").hidden = true;
    $("previous-page").disabled = true;
    $("next-page").disabled = true;
    const query = new URLSearchParams({ page: String(page), size: "8" });
    if (state.keyword) query.set("keyword", state.keyword);
    try {
      const data = await fetchData("/api/products?" + query.toString());
      if (sequence !== state.catalogSequence) return;
      if (!Number.isSafeInteger(data.page) || !Number.isSafeInteger(data.totalPages) || !Number.isSafeInteger(data.total)) {
        throw new ApiError("商品列表信息不完整，请稍后再试。");
      }
      renderCards(catalogGrid, data.items);
      state.catalogPage = data.page;
      state.catalogPages = data.totalPages;
      $("catalog-total").textContent = data.total + " 件";
      $("catalog-total").hidden = false;
      $("catalog-status").textContent = data.items.length ? "" : "暂无匹配商品，换个关键词试试。";
      $("pagination").hidden = data.totalPages <= 1;
      $("page-label").textContent = "第 " + data.page + " / " + Math.max(1, data.totalPages) + " 页";
    } catch (error) {
      if (sequence !== state.catalogSequence) return;
      catalogGrid.replaceChildren();
      $("catalog-total").hidden = true;
      $("pagination").hidden = true;
      $("catalog-status").textContent = friendlyError(error);
      $("catalog-status").classList.add("is-error");
      $("catalog-retry").hidden = false;
    } finally {
      if (sequence === state.catalogSequence) {
        catalogGrid.setAttribute("aria-busy", "false");
        $("previous-page").disabled = state.catalogPage <= 1;
        $("next-page").disabled = state.catalogPage >= state.catalogPages;
      }
    }
  }

  function searchCatalog(keyword) {
    state.keyword = keyword.trim();
    $("catalog-keyword").value = state.keyword;
    document.querySelectorAll("[data-keyword]").forEach((button) => {
      const active = button.dataset.keyword === state.keyword;
      button.classList.toggle("is-active", active);
      button.setAttribute("aria-pressed", String(active));
    });
    loadCatalog(1);
  }

  $("catalog-search-form").addEventListener("submit", (event) => {
    event.preventDefault();
    searchCatalog($("catalog-keyword").value);
  });
  document.querySelectorAll("[data-keyword]").forEach((button) => {
    button.addEventListener("click", () => searchCatalog(button.dataset.keyword));
  });
  $("retry-catalog").addEventListener("click", () => loadCatalog(state.catalogPage));
  $("previous-page").addEventListener("click", () => loadCatalog(state.catalogPage - 1));
  $("next-page").addEventListener("click", () => loadCatalog(state.catalogPage + 1));

  async function openDetail(id) {
    const sequence = ++state.detailSequence;
    const body = $("detail-body");
    body.replaceChildren(textElement("p", "dialog-message", "正在读取商品详情…"));
    if (!dialog.open) dialog.showModal();
    document.body.classList.add("dialog-open");
    try {
      const product = await fetchData("/api/products/" + encodeURIComponent(id));
      if (sequence !== state.detailSequence || !dialog.open) return;
      if (!validProduct(product) || typeof product.description !== "string") throw new ApiError("商品详情不完整，请稍后再试。");
      const layout = textElement("div", "detail-layout");
      const copy = textElement("div", "detail-copy");
      const title = textElement("h2", "", product.title);
      title.id = "detail-title";
      copy.append(
        textElement("p", "product-brand", product.brand),
        title,
        priceElement(product.price),
        textElement("h3", "", "关于这件商品"),
        textElement("p", "detail-description", product.description || "暂无更多商品介绍。"),
        textElement("p", "detail-note", "商品编号 " + product.id + " · 以当前商品信息为准")
      );
      layout.append(productVisual(product), copy);
      body.replaceChildren(layout);
    } catch (error) {
      if (sequence !== state.detailSequence || !dialog.open) return;
      const message = textElement("div", "dialog-message");
      const title = textElement("h2", "", "暂时无法查看商品");
      title.id = "detail-title";
      message.append(title, textElement("p", "", friendlyError(error)));
      body.replaceChildren(message);
    }
  }

  $("close-detail").addEventListener("click", () => dialog.close());
  dialog.addEventListener("close", () => {
    state.detailSequence++;
    document.body.classList.remove("dialog-open");
  });
  dialog.addEventListener("click", (event) => {
    if (event.target !== dialog) return;
    const rect = dialog.getBoundingClientRect();
    if (event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom) dialog.close();
  });

  updateCount();
  loadCatalog();
})();

