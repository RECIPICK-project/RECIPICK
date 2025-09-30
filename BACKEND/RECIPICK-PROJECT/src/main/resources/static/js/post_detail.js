// ====== post_detail.js (최종본) ======

let isLiked = false;
let isLoading = false;
window.currentPostData = null;

/* -------------------------------
 * 공통 유틸 (토큰/CSRF/Fetch)
 * ------------------------------- */
function getCookie(name) {
  return document.cookie
      .split("; ")
      .map((s) => s.trim())
      .find((row) => row.startsWith(name + "="))
      ?.split("=")[1];
}

function authHeader() {
  const token = localStorage.getItem("ACCESS_TOKEN");
  return token ? { Authorization: `Bearer ${token}` } : {};
}

function csrfHeaders(method) {
  const m = (method || "GET").toUpperCase();
  if (["GET", "HEAD", "OPTIONS", "TRACE"].includes(m)) return {};
  const xsrf = getCookie("XSRF-TOKEN");
  return xsrf ? { "X-XSRF-TOKEN": decodeURIComponent(xsrf) } : {};
}

async function fx(path, { method = "GET", headers = {}, body, credentials = "include" } = {}) {
  const isForm = typeof FormData !== "undefined" && body instanceof FormData;
  const h = {
    Accept: "application/json",
    ...authHeader(),
    ...csrfHeaders(method),
    ...headers,
  };
  if (isForm && h["Content-Type"]) delete h["Content-Type"];
  const res = await fetch(path, { method, headers: h, body, credentials });
  return res;
}

/* -------------------------------
 * URL에서 postId 추출
 * ------------------------------- */
function getPostIdFromUrl() {
  const p = new URLSearchParams(location.search).get("postId");
  if (p && /^\d+$/.test(p)) return p;
  const seg = location.pathname.split("/").filter(Boolean);
  const last = seg[seg.length - 1];
  return /^\d+$/.test(last) ? last : null;
}

/* -------------------------------
 * DOMContentLoaded
 * ------------------------------- */
document.addEventListener("DOMContentLoaded", async function () {
  const postId = getPostIdFromUrl();
  if (!postId) {
    alert("잘못된 접근입니다. (postId 없음)");
    return;
  }

  // 상세 불러오기
  await loadRecipeData(postId);

  // 슬라이드 메뉴
  initializeSlideMenu();

  // 좋아요 버튼 초기화
  await initializeLikeButton(postId);

  // 모달 바인딩 보장
  wireReportModal();
});

/* -------------------------------
 * 상세 불러오기
 * ------------------------------- */


async function loadRecipeData(postId) {
  if (isLoading) return;
  isLoading = true; 

  try {
    const res = await fx(`/post/${encodeURIComponent(postId)}`);
    if (res.status === 401) console.warn("401 Unauthorized");
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    
    const json = await res.json();
    const dto = (json && typeof json === "object" && "data" in json) ? json.data : json;
    if (!dto || typeof dto !== "object") throw new Error("응답 형식이 올바르지 않습니다.");

    window.currentPostData = dto; 
    
    const view = normalizeRecipe(dto);
    renderRecipeData(view);
  } catch (e) {
    console.error("레시피 로드 에러:", e);
    alert("레시피를 불러올 수 없습니다.");
  } finally {
    isLoading = false;
  }
}

/* -------------------------------
 * DTO 정규화
 * ------------------------------- */
function normalizeRecipe(dto) {
  const title = dto.title ?? dto.foodName ?? "";
  const author = dto.author ?? dto.nickname ?? dto.userName ?? "";
  const difficulty = dto.difficulty ?? dto.ckgLevel ?? "";
  const servings = dto.servings ?? dto.ckgInbun ?? "";
  const cookingTime = dto.cookingTimeString ?? dto.cookingTime ?? "";
  const thumbnailUrl = dto.rcpImgUrl ?? dto.imageUrl ?? dto.thumb ?? "";
  const ingredients = dto.ckgMtrlCn ?? dto.ingredientsString ?? dto.rcpIngredients ?? "";

  let stepsRaw = dto.rcpSteps ?? dto.steps ?? dto.stepsString ?? [];
  let stepImagesRaw = dto.stepImages ?? dto.stepsImages ?? dto.rcpStepImages ?? dto.rcpStepsImg ?? dto.stepImagesString ?? null;

  const clean = (v) => (v == null ? "" : String(v).replace(/\s+/g, " ").trim());
  function cleanUrl(u) {
    const s = clean(u);
    if (!s) return "";
    if (s.startsWith("//")) return "https:" + s;
    if (/^(data:|https?:|\/)/i.test(s)) return s;
    return "";
  }

  function hasManualFields(o) {
    for (let i = 1; i <= 3; i++) {
      const k2 = String(i).padStart(2, "0");
      if (o["MANUAL" + k2] || o["manual" + k2] || o["MANUAL_" + k2] || o["manual_" + k2] ||
          o["Step" + k2] || o["step" + k2]) return true;
      if (o["MANUAL_IMG" + k2] || o["MANUAL_IMG_" + k2] || o["manual_img" + k2] ||
          o["manual_img_" + k2] || o["manualImg" + k2] || o["ManualImg" + k2] ||
          o["stepImg" + k2] || o["StepImg" + k2]) return true;
    }
    return false;
  }

  function getByVariants(o, i, kind) {
    const k2 = String(i).padStart(2, "0");
    const stepKeys = [`MANUAL${k2}`, `MANUAL_${k2}`, `manual${k2}`, `manual_${k2}`, `Step${k2}`, `step${k2}`];
    const imgKeys = [`MANUAL_IMG${k2}`, `MANUAL_IMG_${k2}`, `manual_img${k2}`, `manual_img_${k2}`, `manualImg${k2}`, `ManualImg${k2}`, `stepImg${k2}`, `StepImg${k2}`];
    const candidates = kind === "img" ? imgKeys : stepKeys;
    for (const k of candidates) if (o[k] != null && clean(o[k])) return clean(o[k]);
    return "";
  }

  function collectManualFields(o) {
    const stepsList = [];
    const imgList = [];
    for (let i = 1; i <= 30; i++) {
      const s = getByVariants(o, i, "step");
      const img = getByVariants(o, i, "img");
      if (!s) continue;
      stepsList.push(s);
      imgList.push(cleanUrl(img));
    }
    return { stepsList, imgList };
  }

  const isStepsEmpty = stepsRaw == null ||
      (Array.isArray(stepsRaw) && stepsRaw.length === 0) ||
      (typeof stepsRaw === "string" && !stepsRaw.trim());

  if (isStepsEmpty && hasManualFields(dto)) {
    const { stepsList, imgList } = collectManualFields(dto);
    stepsRaw = stepsList;
    stepImagesRaw = imgList.some(Boolean) ? imgList : null;
  }

  const steps = parseSteps(stepsRaw, stepImagesRaw);

  return { title, author, difficulty, servings, cookingTime, thumbnailUrl, ingredients, steps };
}

function parseSteps(stepsRaw, stepImagesRaw) {
  const stripLeadingNumber = (txt) => String(txt).replace(/^\s*\d+[.)]\s*/, "").trim();

  const explodePipe = (txt) =>
      String(txt)
          .split("|").map((t) => t.trim()).filter(Boolean)
          .map((t) => ({ description: stripLeadingNumber(t), imageUrl: "" }));

  function normalizeImages(imgRaw) {
    if (!imgRaw) return [];
    if (Array.isArray(imgRaw)) return imgRaw.map((s) => String(s ?? "").trim()).filter(Boolean);
    const s = String(imgRaw).trim();
    if (s.startsWith("[") && s.endsWith("]")) {
      try {
        const arr = JSON.parse(s);
        if (Array.isArray(arr)) return arr.map((x) => String(x ?? "").trim()).filter(Boolean);
      } catch (_) {}
    }
    return s.split(/[|,]/).map((t) => t.trim()).filter(Boolean);
  }

  const imgArr = normalizeImages(stepImagesRaw);

  if (Array.isArray(stepsRaw)) {
    const out = [];
    for (const s of stepsRaw) {
      if (s == null) continue;
      if (typeof s === "string") {
        if (s.includes("|")) out.push(...explodePipe(s));
        else out.push({ description: stripLeadingNumber(s), imageUrl: "" });
      } else if (typeof s === "object") {
        const desc = s.description ?? s.desc ?? s.text ?? s.step ?? s.content ?? "";
        const img = s.imageUrl ?? s.img ?? s.image ?? s.photoUrl ?? s.photo ?? s.url ?? "";
        const cleaned = stripLeadingNumber(desc || "");
        if (cleaned) out.push({ description: cleaned, imageUrl: img || "" });
      } else {
        out.push({ description: stripLeadingNumber(String(s)), imageUrl: "" });
      }
    }
    for (let i = 0; i < out.length; i++) if (!out[i].imageUrl && imgArr[i]) out[i].imageUrl = imgArr[i];
    return out;
  }

  if (typeof stepsRaw === "string") {
    const out = explodePipe(stepsRaw);
    for (let i = 0; i < out.length; i++) if (!out[i].imageUrl && imgArr[i]) out[i].imageUrl = imgArr[i];
    return out;
  }

  return [];
}

/* -------------------------------
 * 렌더링
 * ------------------------------- */
function getEl(...selectors) {
  for (const s of selectors) {
    const el = document.querySelector(s);
    if (el) return el;
  }
  return null;
}

function renderRecipeData(data) {
  const titleEl = getEl('#recipeTitle', '[data-field="title"]', '.recipe-title');
  const authorEl = getEl('#authorName', '[data-field="author"]', '.recipe-author');
  const diffEl = getEl('#difficulty', '[data-field="difficulty"]', '.recipe-difficulty');
  const servEl = getEl('#servings', '[data-field="servings"]', '.recipe-servings');
  const timeEl = getEl('#cookingTime', '[data-field="cookingTime"]', '.recipe-time');

  if (titleEl) titleEl.textContent = data.title || '';
  if (authorEl) authorEl.textContent = data.author || '';
  if (diffEl) diffEl.textContent = data.difficulty || '';
  if (servEl) servEl.textContent = data.servings || '';
  if (timeEl) timeEl.textContent = data.cookingTime || '';

  const thumbBox = getEl('#thumbnailBox', '.thumbnail-box', '[data-field="thumbnail"]');
  if (thumbBox) {
    if (data.thumbnailUrl) {
      thumbBox.innerHTML = `<img src="${data.thumbnailUrl}" alt="레시피 이미지">`;
      thumbBox.classList.add('has-img');
    } else {
      thumbBox.innerHTML = '';
      thumbBox.classList.remove('has-img');
    }
  }

  renderIngredients(data.ingredients);
  renderCookingSteps(data.steps);
}

// === 재료 렌더 ===
function renderIngredients(ingredientsValue) {
  const container = document.getElementById("ingredientsList");
  if (!container) return;
  container.innerHTML = "";
  if (!ingredientsValue) return;

  const FRAC_MAP = { "½":"1/2","⅓":"1/3","⅔":"2/3","¼":"1/4","¾":"3/4","⅕":"1/5","⅖":"2/5","⅗":"3/5","⅘":"4/5","⅙":"1/6","⅚":"5/6","⅛":"1/8","⅜":"3/8","⅝":"5/8","⅞":"7/8" };
  const normalizeFractions = (s) => s.replace(/[\u00BC-\u00BE\u2150-\u215E]/g, (ch) => FRAC_MAP[ch] || ch);

  const normalized = normalizeFractions(String(ingredientsValue))
      .replace(/[\x00-\x1F\x7F]/g, "")
      .replace(/\[재료\]/g, "")
      .replace(/\[[^\]]*\]/g, "|")
      .replace(/[,\u3001\uFF0C\u00B7\u2022\u2219;]/g, "|")
      .replace(/}$/g, "")
      .replace(/^\|+|\|+$/g, "");

  const tokens = normalized.split("|").map(s => s.trim()).filter(Boolean);

  const UNIT_RE = "(장|개|g|kg|mg|L|l|ml|컵|스푼|큰술|작은술|tsp|tbsp|T|모|줌|쪽|꼬집|알|대|봉|팩|마리|줄|공기|조각|덩이|스틱)";
  const NUM_PART = String.raw`[0-9]+(?:[.,][0-9]+)?(?:\s*[\/∕]\s*[0-9]+)?`;
  const AMOUNT_RE = new RegExp(String.raw`^\s*(.+?)\s*(${NUM_PART}(?:\s*~\s*${NUM_PART})?\s*${UNIT_RE}?)\s*$`);
  const FIRST_NUMBER_OR_FRAC = /[0-9]/;

  function splitNameAmount(raw) {
    const s = normalizeFractions(String(raw).replace(/\s+/g, " ").trim());
    if (!s) return { name: "", amount: "" };
    const m = s.match(AMOUNT_RE);
    if (m) return { name: m[1].trim(), amount: m[2].replace(/\s+/g, " ").trim() };
    const idx = s.search(FIRST_NUMBER_OR_FRAC);
    if (idx > 0) return { name: s.slice(0, idx).trim(), amount: s.slice(idx).trim() };
    const k = s.lastIndexOf(" ");
    if (k > 0) return { name: s.slice(0, k).trim(), amount: s.slice(k + 1).trim() };
    return { name: s, amount: "" };
  }

  const items = tokens.map(splitNameAmount);

  const titleEl = document.getElementById("recipeTitle");
  const recipeTitle = titleEl ? titleEl.textContent || "" : "";

  items.forEach(({ name, amount }) => {
    if (!name) return;
    const ingredientItem = document.createElement("div");
    ingredientItem.className = "ingredient-item";
    ingredientItem.innerHTML = `
      <span class="ingredient-name">${name}</span>
      <span class="ingredient-amount">${amount}</span>
      <button class="btn-small btn-substitute"
              onclick="getSubstituteIngredient(this)"
              data-ingredient-name="${name}"
              data-ingredient-amount="${amount}"
              data-recipe-title="${recipeTitle}">대체</button>
      <button class="btn-small" onclick="goToCoupang('${name}')">구매</button>
    `;
    container.appendChild(ingredientItem);
  });
}

function renderCookingSteps(steps) {
  const container = document.getElementById("cookingStepsList");
  if (!container) return;
  container.innerHTML = "";

  (steps || []).forEach((step, idx) => {
    const desc = step?.description ?? String(step ?? "");
    const img = step?.imageUrl ?? "";
    const el = document.createElement("div");
    el.className = "step-item";
    el.innerHTML = `
      <div class="step-header">${idx + 1}단계</div>
      <div class="step-content">
        <div class="step-description">${desc}</div>
        <div class="step-image">${
        img ? `<img src="${img}" alt="${idx + 1}단계 이미지">` : '<div class="step-image-placeholder">사진</div>'
    }</div>
      </div>
    `;
    const imgEl = el.querySelector(".step-image img");
    if (imgEl) {
      imgEl.addEventListener("error", () => {
        el.querySelector(".step-image").innerHTML = '<div class="step-image-placeholder">사진</div>';
      });
    }
    container.appendChild(el);
  });
}

// 대체 재료
function getSubstituteIngredient(buttonElement) {
  buttonElement.innerText = "불러오는 중...";
  buttonElement.disabled = true;

  const ingredientName = buttonElement.dataset.ingredientName;
  const amount = buttonElement.dataset.ingredientAmount;
  const recipeTitle = buttonElement.dataset.recipeTitle;

  const params = new URLSearchParams({ ingredientName, amount, title: recipeTitle });
  const serverUrl = `/api/substitute-ingredient?${params.toString()}`;

  fetch(serverUrl, {
    method: 'GET',
    headers: { 'Accept': 'application/json, text/plain, */*', 'Content-Type': 'application/json' }
  })
      .then(response => {
        if (!response.ok) {
          if (response.status === 404) throw new Error('API 엔드포인트를 찾을 수 없습니다.');
          if (response.status === 500) throw new Error('서버 내부 오류가 발생했습니다.');
          throw new Error(`서버 응답 오류: ${response.status} ${response.statusText}`);
        }
        return response.text();
      })
      .then(data => {
        if (data.startsWith('[에러]') || data.startsWith('[예외 발생]')) throw new Error(data);
        buttonElement.innerText = data || '대체재료 없음';
        buttonElement.disabled = false;
        buttonElement.style.backgroundColor = '#28a745';
        buttonElement.style.color = 'white';
        setTimeout(() => { buttonElement.style.backgroundColor = ''; buttonElement.style.color = ''; }, 3000);
      })
      .catch(error => {
        let errorMessage = '오류 발생';
        if (error.message.includes('404')) errorMessage = 'API 없음';
        else if (error.message.includes('500')) errorMessage = '서버 오류';
        else if (error.message.includes('Failed to fetch')) errorMessage = '네트워크 오류';
        buttonElement.innerText = errorMessage;
        buttonElement.disabled = false;
        buttonElement.style.backgroundColor = '#dc3545';
        buttonElement.style.color = 'white';
        setTimeout(() => { buttonElement.innerText = '대체'; buttonElement.style.backgroundColor = ''; buttonElement.style.color = ''; }, 5000);
      });
}

/* -------------------------------
 * 슬라이드 메뉴
 * ------------------------------- */
function initializeSlideMenu() {
  const slideMenu = document.getElementById("slideMenu");
  if (!slideMenu) return;
  slideMenu.addEventListener("click", function (e) {
    if (e.target.classList.contains("slide-menu-overlay")) closeSlideMenu();
  });
}
function toggleSlideMenu() {
  const slideMenu = document.getElementById("slideMenu");
  if (!slideMenu) return;
  slideMenu.classList.toggle("active");
  document.body.style.overflow = slideMenu.classList.contains("active") ? "hidden" : "";
}
function closeSlideMenu() {
  const slideMenu = document.getElementById("slideMenu");
  if (!slideMenu) return;
  slideMenu.classList.remove("active");
  document.body.style.overflow = "";
}
document.addEventListener("keydown", function (e) {
  if (e.key === "Escape") closeSlideMenu();
});
function handleLogout() {
  if (confirm("로그아웃하시겠습니까?")) {
    localStorage.clear();
    alert("로그아웃되었습니다.");
    location.href = "/login";
  }
}
function goToPage(url) { closeSlideMenu(); location.href = url; }
function setActiveTab(tabName) {
  document.querySelectorAll(".tab-item").forEach((el) => {
    el.classList.toggle("active", el.dataset.tab === tabName);
  });
}

/* -------------------------------
 * 좋아요 (서버 연동)
 * ------------------------------- */
async function initializeLikeButton(postId) {
  try {
    const res = await fx(`/post/${encodeURIComponent(postId)}/like`, { method: "GET" });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    isLiked = await res.json();
  } catch (e) {
    console.warn("좋아요 상태 조회 실패:", e);
    isLiked = false;
  } finally {
    updateLikeButtonState();
    const likeButton = document.getElementById("likeButton");
    if (likeButton) likeButton.onclick = () => toggleLike(postId);
  }
}
async function toggleLike(postId) {
  isLiked = !isLiked;
  updateLikeButtonState();
  try {
    const method = isLiked ? "POST" : "DELETE";
    const res = await fx(`/post/${encodeURIComponent(postId)}/like`, { method });
    if (res.status === 401) throw new Error("401 Unauthorized");
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
  } catch (e) {
    console.error("좋아요 처리 실패:", e);
    isLiked = !isLiked;
    updateLikeButtonState();
    alert("좋아요 처리 중 오류가 발생했습니다.");
  }
}
function updateLikeButtonState() {
  const likeButton = document.getElementById("likeButton");
  if (!likeButton) return;
  likeButton.classList.toggle("liked", !!isLiked);
}

/* ===============================
 * 신고 (Report)
 * =============================== */
const REPORT_API = '/admin/reports';
const REPORT_TARGET = 'POST';

function openReportModal() {
  const modal = document.getElementById('reportModal');
  if (!modal) return;
  modal.classList.remove('hidden');
}

function closeReportModal() {
  const modal = document.getElementById('reportModal');
  if (!modal) return;
  modal.classList.add('hidden');
}

// ★ 즉시 실행 함수를 일반 함수로 변경
function wireReportModal() {
  const modal = document.getElementById('reportModal');
  if (!modal) return;

  // 닫기 처리 (X, 취소, 오버레이)
  modal.addEventListener('click', (e)=>{
    if (e.target.matches('[data-close], [data-close] *') || e.target.classList.contains('report-overlay')) {
      closeReportModal();
    }
  });

  // Esc로 닫기
  document.addEventListener('keydown', (e)=>{
    if (e.key === 'Escape') closeReportModal();
  });

  // 제출
  const submitBtn = document.getElementById('submitReportBtn');
  if (submitBtn) {
    submitBtn.addEventListener('click', submitReport);
  }
}

async function submitReport() {
  const postId = getPostIdFromUrl();
  if (!postId) return alert('잘못된 접근입니다.');

  const token = localStorage.getItem('ACCESS_TOKEN');
  if (!token) {
    alert('로그인이 필요합니다.');
    console.warn('[REPORT] ACCESS_TOKEN not found in localStorage');
    return;
  }

  const reasonEl = document.getElementById('reportReason');
  const reason = (reasonEl?.value || '').trim();
  if (!reason) return alert('신고 사유를 선택해주세요.');

  try {
    console.log('[REPORT] endpoint =', REPORT_API);
    console.log('[REPORT] token length =', token.length);

    const res = await fx(REPORT_API, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        targetType: REPORT_TARGET,
        targetId: Number(postId),
        reason
      })
    });

    console.log('[REPORT] status =', res.status);
    if (res.status === 401) {
      const body = await res.text().catch(()=> '');
      console.warn('[REPORT] 401 body =', body);
      throw new Error('401');
    }
    if (!res.ok) {
      const body = await res.text().catch(()=> '');
      console.warn('[REPORT] non-OK body =', body);
      throw new Error('HTTP ' + res.status);
    }

    alert('신고가 접수되었습니다. 감사합니다.');
    closeReportModal();
    if (reasonEl) reasonEl.value = '';
  } catch (e) {
    console.error('신고 오류:', e);
    if (String(e.message).includes('401')) alert('로그인이 필요합니다.');
    else alert('신고 처리 중 오류가 발생했습니다.');
  }
}