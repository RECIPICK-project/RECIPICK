// ====== post_detail.js (live) ======
// - 상세: GET /post/{postId} (PostRestController#getRecipeById)
//   응답: { success: true, data: { ...PostDto } }  또는  바로 { ...PostDto }
// - 좋아요: /post/{postId}/like  (GET: 여부, POST: 좋아요, DELETE: 취소)

let isLiked = false;

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
 * URL에서 postId 추출 ( /post_detail/123  또는  ?postId=123 )
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

  // 탭바 표시 갱신이 필요하면 setActiveTab('home') 같은 거 호출
});

/* -------------------------------
 * 상세 불러오기
 * ------------------------------- */
async function loadRecipeData(postId) {
  try {
    const res = await fx(`/post/${encodeURIComponent(postId)}`);
    if (res.status === 401) {
      console.warn("401 Unauthorized");
      // location.href = '/pages/login.html';
      // return;
    }
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const json = await res.json();

    // ★ 응답 래핑/비래핑 모두 지원
    const dto = (json && typeof json === "object" && "data" in json)
        ? json.data
        : json;

    if (!dto || typeof dto !== "object") {
      throw new Error("응답 형식이 올바르지 않습니다.");
    }

    // 서버 DTO → 화면 데이터로 정규화
    const view = normalizeRecipe(dto);
    renderRecipeData(view);
  } catch (e) {
    console.error("레시피 로드 에러:", e);
    alert("레시피를 불러올 수 없습니다.");
  }
}

/* -------------------------------
 * DTO 정규화
 * ------------------------------- */
function normalizeRecipe(dto) {
  const title =
      dto.title ??
      dto.foodName ??
      "";
  const author =
      dto.author ??
      dto.nickname ??
      dto.userName ??
      "";
  const difficulty =
      dto.difficulty ??
      dto.ckgLevel ??
      "";
  const servings =
      dto.servings ??
      dto.ckgInbun ??
      "";
  const cookingTime =
      dto.cookingTimeString ??
      dto.cookingTime ??
      "";
  const thumbnailUrl =
      dto.rcpImgUrl ??
      dto.imageUrl ??
      dto.thumb ??
      "";

  // 재료 문자열: "이름|수량|이름|수량|..."
  const ingredients =
      dto.ckgMtrlCn ??                 // ★ 추가
      dto.ingredientsString ??
      dto.rcpIngredients ??
      "";

  // 단계 & (선택) 단계 이미지 별도 필드 대응
  let stepsRaw =
      dto.rcpSteps ??
      dto.steps ??
      dto.stepsString ??
      [];

  // 이미지가 별도 배열/문자열로 오는 경우 커버
  const stepImagesRaw =
      dto.stepImages ??
      dto.stepsImages ??
      dto.rcpStepImages ??
      dto.rcpStepsImg ??
      dto.stepImagesString ??
      null;

  const steps = parseSteps(stepsRaw, stepImagesRaw);

  return {
    title,
    author,
    difficulty,
    servings,
    cookingTime,
    thumbnailUrl,
    ingredients,
    steps,
  };
}

// 교체: parseSteps (번호 제거 + 이미지 매칭 강화: 배열/문자열/JSON/콤마 모두 지원)
function parseSteps(stepsRaw, stepImagesRaw) {
  const stripLeadingNumber = (txt) =>
      String(txt).replace(/^\s*\d+[.)]\s*/, "").trim();

  const explodePipe = (txt) =>
      String(txt)
          .split("|")
          .map((t) => t.trim())
          .filter(Boolean)
          .map((t) => ({ description: stripLeadingNumber(t), imageUrl: "" }));

  // 이미지 목록을 어떤 형태든 배열로 정규화
  function normalizeImages(imgRaw) {
    if (!imgRaw) return [];
    if (Array.isArray(imgRaw)) {
      return imgRaw.map((s) => String(s ?? "").trim()).filter(Boolean);
    }
    const s = String(imgRaw).trim();
    // JSON 문자열 ["url1","url2"]
    if (s.startsWith("[") && s.endsWith("]")) {
      try {
        const arr = JSON.parse(s);
        if (Array.isArray(arr)) {
          return arr.map((x) => String(x ?? "").trim()).filter(Boolean);
        }
      } catch (_) {}
    }
    // | 또는 , 로 구분된 문자열
    return s.split(/[|,]/).map((t) => t.trim()).filter(Boolean);
  }

  const imgArr = normalizeImages(stepImagesRaw);

  // 1) 배열로 온 경우
  if (Array.isArray(stepsRaw)) {
    const out = [];
    for (const s of stepsRaw) {
      if (s == null) continue;

      if (typeof s === "string") {
        if (s.includes("|")) out.push(...explodePipe(s));
        else out.push({ description: stripLeadingNumber(s), imageUrl: "" });
      } else if (typeof s === "object") {
        const desc =
            s.description ?? s.desc ?? s.text ?? s.step ?? s.content ?? "";
        const img =
            s.imageUrl ?? s.img ?? s.image ?? s.photoUrl ?? s.photo ?? s.url ?? "";
        const cleaned = stripLeadingNumber(desc || "");
        if (cleaned) out.push({ description: cleaned, imageUrl: img || "" });
      } else {
        out.push({ description: stripLeadingNumber(String(s)), imageUrl: "" });
      }
    }
    // 비어있는 이미지 칸에만 보조 배열로 주입
    for (let i = 0; i < out.length; i++) {
      if (!out[i].imageUrl && imgArr[i]) out[i].imageUrl = imgArr[i];
    }
    return out;
  }

  // 2) 문자열로 온 경우 ("1. 준비|2. 확인 필요" 등)
  if (typeof stepsRaw === "string") {
    const out = explodePipe(stepsRaw);
    for (let i = 0; i < out.length; i++) {
      if (!out[i].imageUrl && imgArr[i]) out[i].imageUrl = imgArr[i];
    }
    return out;
  }

  return [];
}




/* -------------------------------
 * 렌더링
 * ------------------------------- */
// 헬퍼: 여러 후보 셀렉터 중 첫 매칭
function getEl(...selectors) {
  for (const s of selectors) {
    const el = document.querySelector(s);
    if (el) return el;
  }
  return null;
}
function renderRecipeData(data) {
  // 제목/작성자/난이도/인분/시간 — id가 다르면 data-field나 클래스도 시도
  const titleEl = getEl('#recipeTitle', '[data-field="title"]', '.recipe-title');
  const authorEl = getEl('#authorName', '[data-field="author"]', '.recipe-author');
  const diffEl   = getEl('#difficulty', '[data-field="difficulty"]', '.recipe-difficulty');
  const servEl   = getEl('#servings',   '[data-field="servings"]',   '.recipe-servings');
  const timeEl   = getEl('#cookingTime','[data-field="cookingTime"]','.recipe-time');

  if (titleEl)  titleEl.textContent = data.title || '';
  if (authorEl) authorEl.textContent = data.author || '';
  if (diffEl)   diffEl.textContent   = data.difficulty || '';
  if (servEl)   servEl.textContent   = data.servings || '';
  if (timeEl)   timeEl.textContent   = data.cookingTime || '';

  // 썸네일
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

// 교체: renderIngredients (콤마/세미콜론/중점/슬래시 지원 + 수량 자동 추출)
function renderIngredients(ingredientsValue) {
  const container = document.getElementById("ingredientsList");
  if (!container) return;
  container.innerHTML = "";

  if (!ingredientsValue) return;

  // 0) 구분자 통합: | , 、 ， · • ∙ ㆍ ; / 를 모두 파이프로 정규화
  const normalized = String(ingredientsValue)
      .replace(/[,\u3001\uFF0C\u00B7\u2022\u2219\u318D;\/]/g, "|");

  const tokens = normalized.split("|").map(s => s.trim()).filter(Boolean);

  // 1) "이름|수량|이름|수량" 페어 형태인지 감지 (짝수길이 + 짝수 인덱스에 이름 느낌)
  const looksLikePairs = tokens.length % 2 === 0;

  const UNIT_RE = "(장|개|g|kg|mg|L|l|ml|컵|스푼|큰술|작은술|tsp|tbsp|모|줌|쪽|꼬집|알|대|봉|팩|마리|줄|공기|조각|장분|덩이|스틱|줌가량)";
  const AMOUNT_RE = new RegExp(
      String.raw`^\s*(.+?)\s*([0-9]+(?:\.[0-9]+)?\s*${UNIT_RE}?(?:\s*~\s*[0-9]+(?:\.[0-9]+)?\s*${UNIT_RE}?)?)\s*$`
  );

  /** 이름/수량 추출기
   *  - "식빵 2장" → {name:"식빵", amount:"2장"}
   *  - "콩나물 3 g" → {name:"콩나물", amount:"3 g"}
   *  - "두부" → {name:"두부", amount:""}
   */
  const splitNameAmount = (s) => {
    const m = s.match(AMOUNT_RE);
    if (m) return { name: m[1].trim(), amount: m[2].replace(/\s+/g, " ").trim() };
    // 마지막 공백 기준 분리(대충이라도)
    const k = s.lastIndexOf(" ");
    if (k > 0) return { name: s.slice(0, k).trim(), amount: s.slice(k + 1).trim() };
    return { name: s.trim(), amount: "" };
  };

  const items = [];
  if (looksLikePairs) {
    for (let i = 0; i < tokens.length; i += 2) {
      const name = tokens[i];
      const amount = tokens[i + 1] || "";
      if (name) items.push({ name, amount });
    }
  } else {
    // "식빵 3장,청경채 2개,상추 1장,콩나물 3g,두부" 같은 콤마 나열 처리
    for (const tok of tokens) {
      items.push(splitNameAmount(tok));
    }
  }

  for (const { name, amount } of items) {
    const safeNameArg = JSON.stringify(String(name));
    const el = document.createElement("div");
    el.className = "ingredient-item";
    el.innerHTML = `
      <span class="ingredient-name">${name}</span>
      <span class="ingredient-amount">${amount}</span>
      <button class="btn-small btn-substitute" onclick="getSubstituteIngredient(${safeNameArg}, this)">대체</button>
      <button class="btn-small" onclick="goToCoupang(${safeNameArg})">구매</button>
    `;
    container.appendChild(el);
  }
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
        img
            ? `<img src="${img}" alt="${idx + 1}단계 이미지">`
            : '<div class="step-image-placeholder">사진</div>'
    }</div>
      </div>
    `;
    // onerror 대체
    const imgEl = el.querySelector(".step-image img");
    if (imgEl) {
      imgEl.addEventListener("error", () => {
        console.warn("[image error]", img);
        el.querySelector(".step-image").innerHTML = '<div class="step-image-placeholder">사진</div>';
      });
    }
    container.appendChild(el);
  });
}


/* -------------------------------
 * 대체 재료 / 쿠팡
 * ------------------------------- */
async function getSubstituteIngredient(ingredientName, button) {
  try {
    button.disabled = true;
    button.textContent = "로딩...";
    // TODO: 서버 연동 시 교체
    await new Promise((r) => setTimeout(r, 600));
    alert(
        `${ingredientName} 대신 쓸 수 있는 대체 재료 예시\n\n1) 비슷한 맛\n2) 영양 유사\n3) 식감 유사`
    );
  } catch (e) {
    console.error(e);
    alert("대체 재료 추천에 실패했습니다.");
  } finally {
    button.disabled = false;
    button.textContent = "대체";
  }
}

function goToCoupang(ingredientName) {
  const url =
      "https://www.coupang.com/np/search?component=&q=" + encodeURIComponent(ingredientName);
  window.open(url, "_blank");
}

/* -------------------------------
 * 슬라이드 메뉴
 * ------------------------------- */
function initializeSlideMenu() {
  const slideMenu = document.getElementById("slideMenu");
  if (!slideMenu) return;
  slideMenu.addEventListener("click", function (e) {
    if (e.target.classList.contains("slide-menu-overlay")) {
      closeSlideMenu();
    }
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

function goToPage(url) {
  closeSlideMenu();
  location.href = url;
}

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
    isLiked = await res.json(); // 서버: true/false
  } catch (e) {
    console.warn("좋아요 상태 조회 실패(비로그인 또는 오류):", e);
    isLiked = false;
  } finally {
    updateLikeButtonState();
    const likeButton = document.getElementById("likeButton");
    if (likeButton) {
      likeButton.onclick = () => toggleLike(postId);
    }
  }
}

async function toggleLike(postId) {
  // 낙관적 UI 적용
  isLiked = !isLiked;
  updateLikeButtonState();

  try {
    const method = isLiked ? "POST" : "DELETE";
    const res = await fx(`/post/${encodeURIComponent(postId)}/like`, { method });
    if (res.status === 401) {
      // location.href = '/pages/login.html';
      throw new Error("401 Unauthorized");
    }
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
  } catch (e) {
    console.error("좋아요 처리 실패:", e);
    // 롤백
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