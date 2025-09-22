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
 * DTO 정규화  (강화본: 다양한 MANUAL 키/URL 정리)
 * ------------------------------- */
function normalizeRecipe(dto) {
  const title =
      dto.title ?? dto.foodName ?? "";
  const author =
      dto.author ?? dto.nickname ?? dto.userName ?? "";
  const difficulty =
      dto.difficulty ?? dto.ckgLevel ?? "";
  const servings =
      dto.servings ?? dto.ckgInbun ?? "";
  const cookingTime =
      dto.cookingTimeString ?? dto.cookingTime ?? "";
  const thumbnailUrl =
      dto.rcpImgUrl ?? dto.imageUrl ?? dto.thumb ?? "";

  const ingredients =
      dto.ckgMtrlCn ??
      dto.ingredientsString ??
      dto.rcpIngredients ??
      "";

  // 1) 기본 경로 (배열/문자열)
  let stepsRaw =
      dto.rcpSteps ??
      dto.steps ??
      dto.stepsString ??
      [];

  let stepImagesRaw =
      dto.stepImages ??
      dto.stepsImages ??
      dto.rcpStepImages ??
      dto.rcpStepsImg ??
      dto.stepImagesString ??
      null;

  // === 유틸: 값 정리(트림 + 비어있으면 "")
  const clean = (v) => {
    if (v == null) return "";
    return String(v).replace(/\s+/g, " ").trim();
  };

  // === 유틸: 이미지 URL 정리
  function cleanUrl(u) {
    const s = clean(u);
    if (!s) return "";
    if (s.startsWith("//")) return "https:" + s;           // 프로토콜 없는 경우
    // data:, http:, https:, / 로 시작하는 상대경로는 허용
    if (/^(data:|https?:|\/)/i.test(s)) return s;
    // 그 외 이상한 값은 버림
    return "";
  }

  // === 대안 경로: 다양한 키 패턴 스캔
  function hasManualFields(o) {
    // 빠르게 1~3개만 스팟 체크
    for (let i = 1; i <= 3; i++) {
      const k2 = String(i).padStart(2, "0");
      if (
          o["MANUAL" + k2] || o["manual" + k2] ||
          o["MANUAL_" + k2] || o["manual_" + k2] ||
          o["Step" + k2] || o["step" + k2] ||
          o["manual" + k2] || o["manual_" + k2]
      ) return true;
      if (
          o["MANUAL_IMG" + k2] || o["MANUAL_IMG_" + k2] ||
          o["manual_img" + k2] || o["manual_img_" + k2] ||
          o["manualImg" + k2] || o["ManualImg" + k2] ||
          o["stepImg" + k2] || o["StepImg" + k2]
      ) return true;
    }
    // 정 못찾으면 정규식으로 한 번 더
    const keys = Object.keys(o);
    return keys.some(k => /^manual[_]?\d{2}$/i.test(k) || /^manual[_]?img[_]?\d{2}$/i.test(k));
  }

  function getByVariants(o, i, kind) {
    const k2 = String(i).padStart(2, "0");
    // 후보 키들(대·소문자/언더스코어/카멜)
    const stepKeys = [
      `MANUAL${k2}`, `MANUAL_${k2}`,
      `manual${k2}`, `manual_${k2}`,
      `Step${k2}`, `step${k2}`
    ];
    const imgKeys = [
      `MANUAL_IMG${k2}`, `MANUAL_IMG_${k2}`,
      `manual_img${k2}`, `manual_img_${k2}`,
      `manualImg${k2}`, `ManualImg${k2}`,
      `stepImg${k2}`, `StepImg${k2}`
    ];
    const candidates = kind === "img" ? imgKeys : stepKeys;
    for (const k of candidates) {
      if (o[k] != null && clean(o[k])) return clean(o[k]);
    }
    // 정규식 폴백(완전히 다른 케이스까지 긁기)
    const re = kind === "img"
        ? new RegExp(`^manual[_]?img[_]?${k2}$`, "i")
        : new RegExp(`^manual[_]?${k2}$|^step${k2}$`, "i");
    for (const k of Object.keys(o)) {
      if (re.test(k) && clean(o[k])) return clean(o[k]);
    }
    return "";
  }

  function collectManualFields(o) {
    const stepsList = [];
    const imgList = [];
    // 여유 있게 30단계까지 허용
    for (let i = 1; i <= 30; i++) {
      const s = getByVariants(o, i, "step");
      const img = getByVariants(o, i, "img");
      if (!s) continue; // 내용 없는 단계는 스킵
      stepsList.push(s);
      imgList.push(cleanUrl(img));
    }
    return { stepsList, imgList };
  }

  const isStepsEmpty =
      stepsRaw == null ||
      (Array.isArray(stepsRaw) && stepsRaw.length === 0) ||
      (typeof stepsRaw === "string" && !stepsRaw.trim());

  if (isStepsEmpty && hasManualFields(dto)) {
    const { stepsList, imgList } = collectManualFields(dto);
    stepsRaw = stepsList;
    // 하나라도 이미지가 있으면 배열 전달, 전부 빈값이면 null → parseSteps 보조 주입 사용
    stepImagesRaw = imgList.some(Boolean) ? imgList : null;
  }

  const steps = parseSteps(stepsRaw, stepImagesRaw);

  // 디버그: 실제 매핑 결과 확인(필요 없으면 주석)
  console.debug("[RECIPICK] steps:", steps);

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

// === 교체: renderIngredients (분수/붙어쓰기 대응) ===
function renderIngredients(ingredientsValue) {
  const container = document.getElementById("ingredientsList");
  if (!container) return;
  container.innerHTML = "";
  if (!ingredientsValue) return;

  // 0) 유니코드 분수 → ASCII "a/b" 로 정규화
  const FRAC_MAP = {
    "½": "1/2", "⅓": "1/3", "⅔": "2/3",
    "¼": "1/4", "¾": "3/4",
    "⅕": "1/5", "⅖": "2/5", "⅗": "3/5", "⅘": "4/5",
    "⅙": "1/6", "⅚": "5/6",
    "⅛": "1/8", "⅜": "3/8", "⅝": "5/8", "⅞": "7/8"
  };
  function normalizeFractions(s) {
    return s.replace(/[\u00BC-\u00BE\u2150-\u215E]/g, (ch) => FRAC_MAP[ch] || ch);
  }

  // 1) 구분자 통합 (대괄호 라벨을 구분자로 취급, 콤마/중점류를 |로)
  const normalized = normalizeFractions(String(ingredientsValue))
      .replace(/[\x00-\x1F\x7F]/g, "")
      .replace(/\[재료\]/g, "")
      .replace(/\[[^\]]*\]/g, "|")
      .replace(/[,\u3001\uFF0C\u00B7\u2022\u2219;]/g, "|")
      .replace(/}$/g, "")
      .replace(/^\|+|\|+$/g, "");

  const tokens = normalized.split("|").map(s => s.trim()).filter(Boolean);

  // 2) 단위/수량 인식 (분수/범위/소수/붙어쓰기)
  //   - UNIT에 'T' 추가
  const UNIT_RE = "(장|개|g|kg|mg|L|l|ml|컵|스푼|큰술|작은술|tsp|tbsp|T|모|줌|쪽|꼬집|알|대|봉|팩|마리|줄|공기|조각|덩이|스틱)";
  //   - 수(소수 가능) [ / 수 ] 가능, 또는 (이미 normalize된) a/b 단독도 허용
  const NUM_PART = String.raw`[0-9]+(?:[.,][0-9]+)?(?:\s*[\/⁄]\s*[0-9]+)?`;
  const AMOUNT_RE = new RegExp(
      String.raw`^\s*(.+?)\s*(${NUM_PART}(?:\s*~\s*${NUM_PART})?\s*${UNIT_RE}?)\s*$`
  );

  // 숫자/분수 시작 인덱스 찾기 (이름과 수량이 붙은 경우: "두부1/4모")
  const FIRST_NUMBER_OR_FRAC = /[0-9]/; // 유니코드 분수는 위에서 ASCII로 변환됨

  function splitNameAmount(raw) {
    const s = normalizeFractions(String(raw).replace(/\s+/g, " ").trim());
    if (!s) return { name: "", amount: "" };

    // 1) 정규식으로 먼저 시도 (이름/수량 사이 공백 유무 상관없이 동작)
    const m = s.match(AMOUNT_RE);
    if (m) {
      return { name: m[1].trim(), amount: m[2].replace(/\s+/g, " ").trim() };
    }

    // 2) 숫자/분수가 등장하는 첫 위치로 분리 (붙어쓰기 케이스)
    const idx = s.search(FIRST_NUMBER_OR_FRAC);
    if (idx > 0) {
      const name = s.slice(0, idx).trim();
      const amount = s.slice(idx).trim();
      return { name, amount };
    }

    // 3) 마지막 공백 기준 분리(최후의 보루)
    const k = s.lastIndexOf(" ");
    if (k > 0) return { name: s.slice(0, k).trim(), amount: s.slice(k + 1).trim() };

    return { name: s, amount: "" };
  }

  const items = tokens.map(splitNameAmount);

        const recipeTitle = document.getElementById("recipeTitle").textContent;

        const ingredientItem = document.createElement("div");
        ingredientItem.className = "ingredient-item";
        ingredientItem.innerHTML = `
      <span class="ingredient-name">${name}</span>
      <span class="ingredient-amount">${amount}</span>
      <button class="btn-small btn-substitute"
                    onclick="getSubstituteIngredient(this)"
                    data-ingredient-name="${name}"
                    data-recipe-title="${recipeTitle}"
                    >대체</button>
      <button class="btn-small" onclick="goToCoupang('${name}')">구매</button>
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

// GPT API를 통한 대체 재료 추천
function getSubstituteIngredient(buttonElement) {
    // 버튼을 비활성화하고 로딩 상태 표시
    buttonElement.innerText = "불러오는 중...";
    buttonElement.disabled = true;

    const ingredientName = buttonElement.dataset.ingredientName;
    const recipeTitle = buttonElement.dataset.recipeTitle;

    // 스프링 컨트롤러의 URL
    const serverUrl = `/api/substitute-ingredient?ingredientName=${ingredientName}&title=${recipeTitle}`;

    fetch(serverUrl)
        .then(response => {
            if (!response.ok) {
                throw new Error('서버 응답 오류: ' + response.status);
            }
            return response.text(); // 응답이 text 형식이므로 .text() 사용
        })
        .then(data => {
            // GPT 응답으로 버튼 텍스트를 변경
            buttonElement.innerText = data;
            buttonElement.disabled = false; // 버튼 다시 활성화
        })
        .catch(error => {
            console.error('Fetch 에러:', error);
            buttonElement.innerText = "오류 발생";
            buttonElement.disabled = false;
        });

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